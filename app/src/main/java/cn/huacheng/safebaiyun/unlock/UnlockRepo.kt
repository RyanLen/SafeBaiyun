package cn.huacheng.safebaiyun.unlock

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import cn.huacheng.safebaiyun.util.ContextHolder
import cn.huacheng.safebaiyun.util.LockBiz
import cn.huacheng.safebaiyun.util.showToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import java.util.UUID

/** A connection owns an immutable door snapshot; callbacks cannot use another door's key. */
@SuppressLint("MissingPermission")
object UnlockRepo {
    private val handler = Handler(Looper.getMainLooper())
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    val logFlow = MutableStateFlow<List<String>>(emptyList())
    private var session: Session? = null
    private val serviceId = UUID.fromString("14839ac4-7d7e-415c-9a42-167340cf2339")
    private val cccdId = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    fun unlock(door: Door? = DataRepo.defaultDoor()) {
        handler.post {
            if (session != null) {
                showToast("正在连接门禁，请稍候")
                return@post
            }
            if (door == null) {
                showToast("请先添加门禁")
                return@post
            }
            val error = doorValidationError(door.name, door.mac, door.key)
            if (error != null) {
                showToast(error)
                return@post
            }
            val current = Session(door.copy(mac = normalizeMac(door.mac)))
            session = current
            mutableBusy.value = true
            current.start()
        }
    }

    private class Session(private val door: Door) {
        private var gatt: BluetoothGatt? = null
        private var reader: BluetoothGattCharacteristic? = null
        private var writer: BluetoothGattCharacteristic? = null
        private val descriptors = ArrayDeque<Pair<BluetoothGattDescriptor, ByteArray>>()
        private var finished = false
        private var responseSent = false
        private val timeout = Runnable { finish("连接门禁超时，请靠近门禁重试") }

        fun start() {
            try {
                val manager = ContextHolder.get().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = manager?.adapter
                if (adapter == null || !adapter.isEnabled) {
                    finish("请先开启蓝牙")
                    return
                }
                val device = adapter.getRemoteDevice(door.mac)
                handler.postDelayed(timeout, 10000)
                showToast("正在连接「${door.name}」")
                gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    device.connectGatt(ContextHolder.get(), false, callback, BluetoothDevice.TRANSPORT_LE)
                } else device.connectGatt(ContextHolder.get(), false, callback)
                if (gatt == null) finish("无法连接门禁")
            } catch (_: SecurityException) {
                finish("请授予附近设备权限")
            } catch (_: Exception) {
                finish("蓝牙连接失败，请重试")
            }
        }

        private fun dispatch(block: () -> Unit) {
            handler.post {
                if (!finished) {
                    try { block() }
                    catch (_: SecurityException) { finish("请授予附近设备权限") }
                    catch (_: Exception) { finish("门禁通信失败，请检查配置后重试") }
                }
            }
        }

        private val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) = dispatch {
                if (status != BluetoothGatt.GATT_SUCCESS) finish("蓝牙连接失败（$status）")
                else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (!g.discoverServices()) finish("无法读取门禁服务")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) finish("门禁连接已断开")
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) = dispatch {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    finish("无法读取门禁服务")
                    return@dispatch
                }
                val service = g.getService(serviceId)
                reader = service?.characteristics?.firstOrNull {
                    it.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
                }
                writer = service?.characteristics?.firstOrNull {
                    it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
                }
                if (reader == null || writer == null) {
                    finish("未找到兼容的门禁服务")
                    return@dispatch
                }
                service?.characteristics?.forEach { characteristic ->
                    val notify = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                    val indicate = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
                    if (notify || indicate) {
                        val descriptor = characteristic.getDescriptor(cccdId)
                        if (descriptor != null) {
                            if (!g.setCharacteristicNotification(characteristic, true)) {
                                finish("无法订阅门禁通知")
                                return@dispatch
                            }
                            descriptors.add(descriptor to if (notify) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                        }
                    }
                }
                nextOperation()
            }

            override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) = dispatch {
                if (status == BluetoothGatt.GATT_SUCCESS) nextOperation()
                else finish("门禁通知设置失败")
            }

            override fun onCharacteristicRead(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic,
                value: ByteArray, status: Int) {
                val snapshot = value.copyOf()
                dispatch { sendResponse(snapshot, status) }
            }

            @Deprecated("Used on Android 12 and earlier")
            override fun onCharacteristicRead(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (Build.VERSION.SDK_INT < 33) {
                    val snapshot = characteristic.value?.copyOf() ?: byteArrayOf()
                    dispatch { sendResponse(snapshot, status) }
                }
            }

            override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) = dispatch {
                finish(if (status == BluetoothGatt.GATT_SUCCESS) "开门指令已发送：${door.name}" else "开门指令发送失败")
            }
        }

        @Suppress("DEPRECATION")
        private fun nextOperation() {
            val connection = gatt ?: return
            val next = descriptors.poll()
            if (next != null) {
                next.first.value = next.second
                if (!connection.writeDescriptor(next.first)) finish("门禁通知设置失败")
            } else if (!connection.readCharacteristic(reader!!)) finish("无法读取门禁验证数据")
        }

        @Suppress("DEPRECATION")
        private fun sendResponse(value: ByteArray, status: Int) {
            if (responseSent) return
            if (status != BluetoothGatt.GATT_SUCCESS || value.isEmpty()) {
                finish("门禁验证数据读取失败")
                return
            }
            val data = LockBiz.encryptData(value, LockBiz.hexToByteArray(door.mac), door.key)
            val characteristic = writer ?: return
            responseSent = true
            characteristic.value = data
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            if (gatt?.writeCharacteristic(characteristic) != true) finish("开门指令发送失败")
        }

        private fun finish(message: String) {
            if (finished) return
            finished = true
            handler.removeCallbacks(timeout)
            try { gatt?.disconnect() } catch (_: Exception) { }
            try { gatt?.close() } catch (_: Exception) { }
            gatt = null
            if (session === this) {
                session = null
                mutableBusy.value = false
            }
            showToast(message)
        }
    }
}
