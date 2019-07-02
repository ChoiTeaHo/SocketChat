package Examplezz14_NIO10_TCPNonBlockingChannel_ä�ü���1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ServerExample extends Application {
	Selector selector;
	ServerSocketChannel serverSocketChannel;
	List<Client> connections = new Vector<Client>();

	// ���κκ�
	public static void main(String[] args) {
		launch(args);
	}

	void startSever() {
		// ���� ���� �ڵ�

		try {
			selector = Selector.open(); // �����͸� �����ߴ�.
			serverSocketChannel = serverSocketChannel.open();
			serverSocketChannel.configureBlocking(false); // Non-Blocking ��� ����.
			serverSocketChannel.bind(new InetSocketAddress(5001));
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); // Selector�� �۾����������

		} catch (Exception e) {
			if (serverSocketChannel.isOpen()) {
				stopServer();
				return;
			}

			Thread thread = new Thread() {
				@Override
				public void run() {
					while (true) {
						try {
							int keyCount = selector.select(); // �۾�ó�� �غ� �� ä���� ���������� ���(Selector����)
							if (keyCount == 0) {
								continue;
							} // wakeUp() ȣ��� select�� 0�� �����ϹǷ� ó������ back
							Set<SelectionKey> selectedKeys = selector.selectedKeys(); // �۾�ó�� �غ񰡵�Ű��
																						// ��� Set�÷������� ����

							Iterator<SelectionKey> iterator = selectedKeys.iterator(); // ���õ�Ű���� �ݺ�
							while (iterator.hasNext()) {
								SelectionKey selectionKey = iterator.next();
								if (selectionKey.isAcceptable()) { // ������� �۾��϶�
									accept(selectionKey);
								} else if (selectionKey.isReadable()) { // �б��۾��϶�
									Client client = (Client) selectionKey.attachment();
									client.receive(selectionKey);
								} else if (selectionKey.isWritable()) { // �����۾��϶�
									Client client = (Client) selectionKey.attachment();
									client.send(selectionKey);
								}
								iterator.remove(); // ���õ� Ű�¿��� ó�� �Ϸ�� SelectionKey�� ����
							}
						} catch (Exception e) {
							if (serverSocketChannel.isOpen()) {
								stopServer();
							}
							break;
						}
					}
				}
			};
			thread.start();

			Platform.runLater(() -> {
				displayText("[���� ����]");
				btnStartStop.setText("stop");
			});

		}
	}

	void stopServer() {
		// ���� ���� �ڵ�
		try {
			Iterator<Client> iterator = connections.iterator();
			while (iterator.hasNext()) {
				Client client = iterator.next();
				client.socketChannel.close();
				iterator.remove();
			}
			if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
				serverSocketChannel.close();
			}

			if (selector != null && selector.isOpen()) {
				selector.close();
			}
			Platform.runLater(() -> {
				displayText("[��������]");
				btnStartStop.setText("start");
			});
		} catch (Exception e) {
		}
	}

	void accept(SelectionKey selectionKey) {
		try {// ä�ΰ�ü ��¹�
			ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
			SocketChannel socketChannel = serverSocketChannel.accept(); // ���ϻ����ϸ� ����

			String message = "[�������: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName()
					+ "]";
			Platform.runLater(() -> displayText(message));

			Client client = new Client(socketChannel);
			connections.add(client);

			Platform.runLater(() -> displayText("���� ����: " + connections.size() + "]"));
		} catch (Exception e) {
			if (serverSocketChannel.isOpen()) {
				stopServer();
			}
		}
	}

	class Client {
		// ������ ��� �ڵ�
		SocketChannel socketChannel;
		String sendData; // Ŭ���̾�Ʈ�� ���� �����͸� �����ϴ� �ʵ�

		Client(SocketChannel socketChannel) {
			try {
				this.socketChannel = socketChannel;
				socketChannel.configureBlocking(false);
				SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
				selectionKey.attach(this);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		void receive(SelectionKey selectionKey) {
			// ������ �ޱ� �ڵ�

			try {
				ByteBuffer byteBuffer = ByteBuffer.allocate(100); // �ʹ��̷�Ʈ��� ���ۻ���
				int readByteCount = socketChannel.read(byteBuffer); // ä���� ���� ���� �����͸� ����

				// Ŭ���̾�Ʈ�� ���������� SocketChannel�� close()�� ȣ������ ���
				if (readByteCount == -1) {
					throw new IOException();
				}

				String message = "[��û ó��: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName()
						+ "]";
				Platform.runLater(() -> displayText(message));

				byteBuffer.flip(); // ������ 0��ġ
				Charset charset = Charset.forName("UTF-8");
				String data = charset.decode(byteBuffer).toString(); // ����Ʈ���� String���� ��ȯ

				for (Client client : connections) {
					client.sendData = data;
					SelectionKey key = client.socketChannel.keyFor(selector);
					key.interestOps(SelectionKey.OP_WRITE); // �۾����� ����
				}
				selector.wakeup(); // ����� �۾������� �����ϵ��� �ϱ����� Selector�� select() ���ŷ�� ���� �� �����
			} catch (Exception e) {
				try {
					connections.remove(Client.this);
					String message = "[Ŭ���̾�Ʈ ��� �ȵ�: " + socketChannel.getRemoteAddress() + ": "
							+ Thread.currentThread().getName() + "]";
					Platform.runLater(() -> displayText(message));
					socketChannel.close();
				} catch (Exception e2) {
				}
			}

		}

		void send(SelectionKey selectionKey) {
			// ������ ���� �ڵ�

			try {
				Charset charset = Charset.forName("UTF-8");
				ByteBuffer byteBuffer = charset.encode(sendData);
				socketChannel.write(byteBuffer); // ������ ������
				selectionKey.interestOps(SelectionKey.OP_READ); // �۾���������
				selector.wakeup(); // ����� �۾������� �����ϵ��� Selector�� selct() ���ŷ����
			} catch (Exception e) {
				try {
					String message = "[Ŭ���̾�Ʈ ��� �ȵ�: " + socketChannel.getRemoteAddress() + ": "
							+ Thread.currentThread().getName() + "]";
					Platform.runLater(() -> displayText(message));
					connections.remove(Client.this);
					socketChannel.close();
				} catch (Exception e2) {
				}
			}

		}
	}

	////////////////////////
	// UI ���� �ڵ�

	TextArea txtDisplay;
	Button btnStartStop;

	@Override
	public void start(Stage primaryStage) throws Exception {
		BorderPane root = new BorderPane();
		root.setPrefSize(500, 300);

		txtDisplay = new TextArea();
		txtDisplay.setEditable(false);
		BorderPane.setMargin(txtDisplay, new Insets(0, 0, 2, 0));
		root.setCenter(txtDisplay);

		btnStartStop = new Button("start");
		btnStartStop.setPrefHeight(30);
		btnStartStop.setMaxWidth(Double.MAX_VALUE);

		btnStartStop.setOnAction(e -> {
			if (btnStartStop.getText().equals("start")) {
				startSever();
			} else if (btnStartStop.getText().equals("stop")) {
				stopServer();
			}
		});

		root.setBottom(btnStartStop);

		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(event -> stopServer());
		primaryStage.show();
	}

	void displayText(String text) {
		txtDisplay.appendText(text + "\n");
	}

}
