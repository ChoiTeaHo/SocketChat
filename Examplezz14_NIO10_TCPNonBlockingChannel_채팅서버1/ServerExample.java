package Examplezz14_NIO10_TCPNonBlockingChannel_채팅서버1;

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

	// 메인부분
	public static void main(String[] args) {
		launch(args);
	}

	void startSever() {
		// 서버 시작 코드

		try {
			selector = Selector.open(); // 셀렉터를 생성했다.
			serverSocketChannel = serverSocketChannel.open();
			serverSocketChannel.configureBlocking(false); // Non-Blocking 방식 선택.
			serverSocketChannel.bind(new InetSocketAddress(5001));
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); // Selector에 작업유형을등록

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
							int keyCount = selector.select(); // 작업처리 준비가 된 채널이 있을때까지 대기(Selector구동)
							if (keyCount == 0) {
								continue;
							} // wakeUp() 호출시 select는 0을 리턴하므로 처음으로 back
							Set<SelectionKey> selectedKeys = selector.selectedKeys(); // 작업처리 준비가된키를
																						// 얻고 Set컬렉션으로 리턴

							Iterator<SelectionKey> iterator = selectedKeys.iterator(); // 선택된키셋의 반복
							while (iterator.hasNext()) {
								SelectionKey selectionKey = iterator.next();
								if (selectionKey.isAcceptable()) { // 연결수락 작업일때
									accept(selectionKey);
								} else if (selectionKey.isReadable()) { // 읽기작업일때
									Client client = (Client) selectionKey.attachment();
									client.receive(selectionKey);
								} else if (selectionKey.isWritable()) { // 쓰기작업일때
									Client client = (Client) selectionKey.attachment();
									client.send(selectionKey);
								}
								iterator.remove(); // 선택된 키셋에서 처리 완료된 SelectionKey를 제거
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
				displayText("[서버 시작]");
				btnStartStop.setText("stop");
			});

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

			if (selector != null && selector.isOpen()) {
				selector.close();
			}
			Platform.runLater(() -> {
				displayText("[서버멈춤]");
				btnStartStop.setText("start");
			});
		} catch (Exception e) {
		}
	}

	void accept(SelectionKey selectionKey) {
		try {// 채널객체 얻는법
			ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
			SocketChannel socketChannel = serverSocketChannel.accept(); // 소켓생성하며 리턴

			String message = "[연결수락: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName()
					+ "]";
			Platform.runLater(() -> displayText(message));

			Client client = new Client(socketChannel);
			connections.add(client);

			Platform.runLater(() -> displayText("연결 개수: " + connections.size() + "]"));
		} catch (Exception e) {
			if (serverSocketChannel.isOpen()) {
				stopServer();
			}
		}
	}

	class Client {
		// 데이터 통신 코드
		SocketChannel socketChannel;
		String sendData; // 클라이언트로 보낼 데이터를 저장하는 필드

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
			// 데이터 받기 코드

			try {
				ByteBuffer byteBuffer = ByteBuffer.allocate(100); // 넌다이렉트방식 버퍼생성
				int readByteCount = socketChannel.read(byteBuffer); // 채널을 통해 받은 데이터를 저장

				// 클라이언트가 정상적으로 SocketChannel의 close()를 호출했을 경우
				if (readByteCount == -1) {
					throw new IOException();
				}

				String message = "[요청 처리: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName()
						+ "]";
				Platform.runLater(() -> displayText(message));

				byteBuffer.flip(); // 포지션 0위치
				Charset charset = Charset.forName("UTF-8");
				String data = charset.decode(byteBuffer).toString(); // 바이트내용 String으로 변환

				for (Client client : connections) {
					client.sendData = data;
					SelectionKey key = client.socketChannel.keyFor(selector);
					key.interestOps(SelectionKey.OP_WRITE); // 작업유형 변경
				}
				selector.wakeup(); // 변경된 작업유형을 감지하도록 하기위해 Selector의 select() 블로킹을 해제 후 재실행
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

		void send(SelectionKey selectionKey) {
			// 데이터 전송 코드

			try {
				Charset charset = Charset.forName("UTF-8");
				ByteBuffer byteBuffer = charset.encode(sendData);
				socketChannel.write(byteBuffer); // 데이터 보내기
				selectionKey.interestOps(SelectionKey.OP_READ); // 작업유형변경
				selector.wakeup(); // 변경된 작업유형을 감지하도록 Selector의 selct() 블로킹해제
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
