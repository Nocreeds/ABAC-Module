package servers;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import abacCore.Core;

public class IoTs extends Thread {
	int qos = 0;
	String[] topics = { "sensor/temp/251640", "sensor/state/281640" };
	String broker = "tcp://localhost:1883";
	String clientId = "hub";
	MemoryPersistence persistence = new MemoryPersistence();
	Core core;

	public IoTs(Core core) {
		this.core = core;
	}

	public void run() {
		try {
			MqttClient client = new MqttClient(broker, clientId, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			client.setCallback(new MqttCallback() {

				@Override
				public void messageArrived(String topic, MqttMessage arg1) throws Exception {
					// System.out.println(topic + ": " + arg1.toString());
					for (String top : topics) {
						if (top.equals(topic)) {
							String[] st = topic.split("/");
							core.write(st[2], st[1], arg1.toString());
						}
					}
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken arg0) {
					System.out.println(arg0);
				}

				@Override
				public void connectionLost(Throwable arg0) {
					System.out.println(arg0);
				}
			});
			connOpts.setCleanSession(true);
			System.out.println("Connecting to broker: " + broker);
			client.connect(connOpts);
			for (String topic : topics)
				client.subscribe(topic, qos);
			System.out.println("Connected");

		} catch (MqttException me) {
			System.out.println("reason " + me.getReasonCode());
			System.out.println("msg " + me.getMessage());
			System.out.println("loc " + me.getLocalizedMessage());
			System.out.println("cause " + me.getCause());
			System.out.println("excep " + me);
			me.printStackTrace();
		}
	}
}
