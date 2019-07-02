package Examplez14_NIO9_TCPBlockingChannel_ä�ü���1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ServerExample extends Application {
	ExecutorService executorService;
	ServerSocketChannel serverSocketChannel;
	List<Client> connections = new Vector<Client>();

	// ���κκ�
	public static void main(String[] args) {
		launch(args);
	}

	void startSever() {
		// ���� ���� �ڵ�
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {
			serverSocketChannel = serverSocketChannel.open();
			serverSocketChannel.configureBlocking(true);
			serverSocketChannel.bind(new InetSocketAddress(5001));

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					Platform.runLater(() -> {
						displayText("[��������]");
						btnStartStop.setText("stop");
					});

					while (true) {
						try {
							SocketChannel socketChannel = serverSocketChannel.accept();
							String message = "[�������: " + socketChannel.getRemoteAddress() + ": "
									+ Thread.currentThread().getName() + "]";
							Platform.runLater(() -> displayText(message));

							Client client = new Client(socketChannel);
							connections.add(client);

							Platform.runLater(() -> displayText("���� ����: " + connections.size() + "]"));
						} catch (Exception e) {
							if (serverSocketChannel.isOpen()) {
								stopServer();
							}
							break;
						}
					}
				}
			};
			executorService.submit(runnable);

		} catch (Exception e) {
			if (serverSocketChannel.isOpen()) {
				stopServer();
			}
			return;
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

			if (executorService != null && !executorService.isShutdown()) {
				executorService.shutdown();
			}
			Platform.runLater(() -> {
				displayText("[��������]");
				btnStartStop.setText("start");
			});
		} catch (Exception e) {
		}
	}

	class Client {
		// ������ ��� �ڵ�
		SocketChannel socketChannel;

		Client(SocketChannel socketChannel) {
			this.socketChannel = socketChannel;
			receive();
		}

		void receive() {
			// ������ �ޱ� �ڵ�
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						while (true) {
							ByteBuffer byteBuffer = ByteBuffer.allocate(100); // �ʹ��̷�Ʈ��� ���ۻ���
							int readByteCount = socketChannel.read(byteBuffer); // ä���� ���� ���� �����͸� ����

							// Ŭ���̾�Ʈ�� ���������� SocketChannel�� close()�� ȣ������ ���
							if (readByteCount == -1) {
								throw new IOException();
							}

							String message = "[��û ó��: " + socketChannel.getRemoteAddress() + ": "
									+ Thread.currentThread().getName() + "]";
							Platform.runLater(() -> displayText(message));

							byteBuffer.flip(); // ������ 0��ġ
							Charset charset = Charset.forName("UTF-8");
							String data = charset.decode(byteBuffer).toString(); // ����Ʈ���� String���� ��ȯ

							for (Client client : connections) {
								client.send(data);
							}
						}
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
			};
			executorService.submit(runnable);
		}

		void send(String data) {
			// ������ ���� �ڵ�
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						Charset charset = Charset.forName("UTF-8");
						ByteBuffer byteBuffer = charset.encode(data);
						socketChannel.write(byteBuffer);

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
			};
			executorService.submit(runnable);
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
