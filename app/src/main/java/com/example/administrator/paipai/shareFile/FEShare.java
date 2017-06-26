package com.example.administrator.paipai.shareFile;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;

import com.example.administrator.paipai.data.BluetoothDeviceDetail;
import com.example.administrator.paipai.utils.MyLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Created by liguo on 2017/1/5.
 */

public class FEShare implements Serializable {
	/**
	 * SPP搜索模式
	 */
	public static final int MODEL_SPP = 0;
	/**
	 * BLE搜索模式
	 */
	public static final int MODEL_BLE = 1;

	// //////////////////////////////////////////////////
	// tab
	public int searchModel = MODEL_SPP;

	public Intent intent = new Intent();
	private boolean waitingForReturn = false;
	public boolean isBleSendFinish = true;
	public boolean isSPP = true;
	public Calendar c_start = Calendar.getInstance();
	public Calendar c_end;
	public Context context;
	public boolean isFinishRefrashList = true;

	public ArrayList<BluetoothDeviceDetail> devices = new ArrayList<BluetoothDeviceDetail>();
	public ArrayList<String> devices_addrs = new ArrayList<String>();

	// 蓝牙
	public BluetoothAdapter bluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();
	public int connect_state = BluetoothProfile.STATE_DISCONNECTED;
	public BluetoothSocket socket;
	public BluetoothDevice device;
	public InputStream inputStream;
	public OutputStream outputStream;

	public ScanCallback scanCallback;
	private Handler mHandler = new Handler() {
	};
	private Runnable stopSPPScanRunnable;
	private List<ScanFilter> filters;

	// 广播信息过滤
	private IntentFilter intentFilter;

	/**********************************************************/

	private static class FEShareHolder {
		// 单例对象实例
		static final FEShare INSTANCE = new FEShare();
	}

	public static FEShare getInstance() {
		return FEShareHolder.INSTANCE;
	}

	// private的构造函数用于避免外界直接使用new来实例化对象
	private FEShare() {
	}

	// readResolve方法应对单例对象被序列化时候
	private Object readResolve() {
		return getInstance();
	}

	/**********************************************************/

	public void init() {
		// 注册广播接收器，接收并处理搜索结果
		context.registerReceiver(receiver, getIntentFilter());
	}

	public void unregisterReceiver() {
		context.unregisterReceiver(receiver);

	}

	private void initSocket() throws IOException {
		final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
		// final String SPP_UUID = "00001102-0000-1000-8000-00805F9B34FB";

		UUID uuid = UUID.fromString(SPP_UUID);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
			socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
		} else {
			socket = device.createRfcommSocketToServiceRecord(uuid);
		}
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
	}

	public IntentFilter getIntentFilter() {
		if (intentFilter == null) {
			intentFilter = new IntentFilter();
			intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
			intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
			intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
			intentFilter
					.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
			// intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
			intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
			intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			intentFilter
					.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
		}
		return intentFilter;
	}

	private void setupRunnable() {
		if (stopSPPScanRunnable == null) {
			stopSPPScanRunnable = new Runnable() {
				@SuppressLint("NewApi")
				@Override
				public void run() {
					bluetoothAdapter.cancelDiscovery();
				}
			};
		}
	}

	/**
	 * 搜索设备 注意：BLE搜索前必须赋值 leScanCallback
	 */
	public boolean search() {
		if (bluetoothAdapter == null)
			return false;
		// 先停止搜索
		stopSearch();
		// 清空数组
		if (devices != null) {
			devices.clear();
			devices_addrs.clear();
		}
		// 设置Runnable
		setupRunnable();

		if (isSPP) {
			mHandler.postDelayed(stopSPPScanRunnable, 30000);// 搜索30s
			// 处理已配对设备
//			Set<BluetoothDevice> pairedDevices = bluetoothAdapter
//					.getBondedDevices();
//			for (BluetoothDevice device : pairedDevices) {
//				// String strShow = "[已配对] " + device.getAddress() + "  " +
//				// device.getName();
//				String strShow = "[已配对] "+ device.getName();
//				addDevice(new BluetoothDeviceDetail(strShow,
//						device.getAddress(), -100));
//				// MyLog.i("添加已配对设备", " ");
//			}
			return bluetoothAdapter.startDiscovery();
		}
		return false;
	}

	synchronized public boolean stopSearch() {
		if (isSPP) {
			return bluetoothAdapter.cancelDiscovery();
		}
		return false;
	}

	/**
	 * SPP连接该地址设备
	 *
	 * @throws IOException
	 */
	private boolean SPPConnect() {
		// device = bluetoothAdapter.getRemoteDevice(address);
		try {
			MyLog.i("initSocket", "------0");
			initSocket();
			MyLog.i("initSocket", "------2");
			// connectingAddr = address;
			sleep(100);
			try {
				socket.connect();
				// connectedAddr = connectingAddr;
				// connectingAddr = null;
				 MyLog.i("连接设备", "成功");
				sleep(100);
				return true;
			} catch (IOException e) {
				 MyLog.i("连接设备", "失败");
				sleep(100);
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	synchronized public boolean connect(BluetoothDevice device) {
		// if (connect_state == BluetoothProfile.STATE_CONNECTED) return false;
		// //打开只支持单连接
		// connect_state = BluetoothProfile.STATE_CONNECTING;
		this.device = device;
		stopSearch();
		if (isSPP) {
			return SPPConnect();
		} else {
			return false;
		}
	}

	/**
	 * 断开连接
	 */
	synchronized public void disConnect() {
		connect_state = BluetoothProfile.STATE_DISCONNECTING;
		new Thread(new Runnable() {
			@SuppressLint("NewApi")
			@Override
			public void run() {
				if (isSPP) {
					if (socket != null) {
						try {
							outputStream.close();
							inputStream.close();
							socket.close();
							socket = null;
							// connectedAddr = null;
							// connectingAddr = null;
							c_start = Calendar.getInstance();
							MyLog.i("断开", "SPP");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {

				}
			}
		}).start();

	}

	/**
	 * 发送数据
	 *
	 * @param b
	 * @return 返回包数
	 */
	public int write(final byte b[]) {
		if (isSPP) {
			int packets = 0;
			int length_bytes = b.length;
			// 分包
			final int perPacketLength = 2000;
			if (length_bytes > perPacketLength) {
				int startPoint = 0;
				byte[] bytes = new byte[perPacketLength];
				while (length_bytes > perPacketLength) {
					System.arraycopy(b, startPoint, bytes, 0, perPacketLength);
					try {
						outputStream.write(bytes);
						startPoint += perPacketLength;
						length_bytes -= perPacketLength;
						packets++;
						MyLog.i("统计一包", "1");
					} catch (IOException e) {
						e.printStackTrace();
						return packets;
					}
				}
				if (length_bytes != perPacketLength) {
					length_bytes = b.length % perPacketLength;
				}
				if (length_bytes > 0) {
					byte[] bytes_last = new byte[length_bytes];
					System.arraycopy(b, startPoint, bytes_last, 0, length_bytes);
					try {
						outputStream.write(bytes_last);
						packets++;
						MyLog.i("统计一包", "2");
					} catch (IOException e) {
						e.printStackTrace();
						return packets;
					}
				}
				return packets;
			} else {
				try {
					outputStream.write(b);
					return 1;
				} catch (IOException e) {
					return 0;
				}
			}
		} else {
			return 0;
		}
	}

	/**
	 * 发送指定范围数据
	 *
	 * @param b
	 * @param off
	 * @param len
	 * @return
	 */
	public int write(byte b[], int off, int len) {
		if (isSPP) {
			if (socket != null && outputStream != null) {
				try {
					outputStream.write(b, off, len);
					return 1;
				} catch (IOException e) {
					return 0;
				}
			} else {
				return 0;
			}
		} else {

			return 0;
		}
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// if (share.tabId != R.id.communication) return;
			final String action = intent.getAction();
			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				connect_state = BluetoothProfile.STATE_CONNECTED;
			} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				connect_state = BluetoothProfile.STATE_DISCONNECTED;
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
			}
		}
	};

	/**
	 * 设置是否等待断开回调
	 *
	 * @param isWait
	 */
	public void setWaitingForReturn(boolean isWait) {
		// waitingForReturn = isWait;
		// if (waitingForReturn){
		// c_start = Calendar.getInstance();
		// }else {
		// c_end = Calendar.getInstance();
		// Toast.makeText(context, "Disconnect: " + String.valueOf
		// (c_end.getTime().getTime() - c_start.getTime().getTime()) + " ms"
		// , Toast.LENGTH_SHORT).show();
		// c_start = c_end;
		// }
	}

	// 搜索模式字符串

	public String setSearchModel(int model) {
		searchModel = model;
		switch (searchModel) {
			case MODEL_SPP: {
				isSPP = true;
				return "SPP";
			}
			case MODEL_BLE: {
				isSPP = false;
				return "BLE";
			}
		}
		return "";
	}

	// 一般搜索模式
	public String model_default() {
		searchModel = isSPP ? MODEL_SPP : MODEL_BLE;
		return setSearchModel(searchModel);
	}

	public void addDevice(BluetoothDeviceDetail deviceDetail) {
		devices.add(deviceDetail);
		devices_addrs.add(deviceDetail.address);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}
}
