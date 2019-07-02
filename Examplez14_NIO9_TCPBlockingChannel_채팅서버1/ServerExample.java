package Examplez14_NIO9_TCPBlockingChannel_채팅서버1;

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

	// 메인부분
	public static void main(String[] args) {
		launch(args);
	}

	void startSever() {
		// 서버 시작 코드
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {
			serverSocketChannel = serverSocketChannel.open();
			serverSocketChannel.configureBlocking(true);
			serverSocketChannel.bind(new InetSocketAddress(5001));

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					Platform.runLater(() -> {
						displayText("[서버시작]");
						btnStartStop.setText("stop");
					});

					while (true) {
						try {
							SocketChannel socketChannel = serverSocketChannel.accept();
							String message = "[연결수락: " + socketChannel.getRemoteAddress() + ": "
									+ Thread.currentThread().getName() + "]";
							Platform.runLater(() -> displayText(message));

							Client client = new Client(socketChannel);
							connections.add(client);

							Platform.runLater(() -> displayText("연결 개수: " + connections.size() + "]"));
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
		// 서버 종료 코드
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
				displayText("[서버멈춤]");
				btnStartStop.setText("start");
			});
		} catch (Exception e) {
		}
	}

	class Client {
		// 데이터 통신 코드
		SocketChannel socketChannel;

		Client(SocketChannel socketChannel) {
			this.socketChannel = socketChannel;
			receive();
		}

		void receive() {
			// 데이터 받기 코드
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						while (true) {
							ByteBuffer byteBuffer = ByteBuffer.allocate(100); // 넌다이렉트방식 버퍼생성
							int readByteCount = socketChannel.read(byteBuffer); // 채널을 통해 받은 데이터를 저장

							// 클라이언트가 정상적으로 SocketChannel의 close()를 호출했을 경우
							if (readByteCount == -1) {
								throw new IOException();
							}

							String message = "[요청 처리: " + socketChannel.getRemoteAddress() + ": "
									+ Thread.currentThread().getName() + "]";
							Platform.runLater(() -> displayText(message));

							byteBuffer.flip(); // 포지션 0위치
							Charset charset = Charset.forName("UTF-8");
							String data = charset.decode(byteBuffer).toString(); // 바이트내용 String으로 변환

							for (Client client : connections) {
								client.send(data);
							}
						}
					} catch (Exception e) {
						try {
							connections.remove(Client.this);
							String message = "[클라이언트 통신 안됨: " + socketChannel.getRemoteAddress() + ": "
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
			// 데이터 전송 코드
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						Charset charset = Charset.forName("UTF-8");
						ByteBuffer byteBuffer = charset.encode(data);
						socketChannel.write(byteBuffer);

					} catch (Exception e) {
						try {
							String message = "[클라이언트 통신 안됨: " + socketChannel.getRemoteAddress() + ": "
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
	// UI 생성 코드

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
