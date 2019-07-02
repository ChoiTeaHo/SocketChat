package Examplez14_NIO9_TCPBlockingChannel_ä�ü���1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ClientExample extends Application {
	SocketChannel socketChannel;

	public static void main(String[] args) {
		launch(args);
	}

	void startClient() {
		// ���� ���� �ڵ�
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					socketChannel = SocketChannel.open();
					socketChannel.configureBlocking(true);
					socketChannel.connect(new InetSocketAddress("localhost", 5001));

					Platform.runLater(() -> {
						try {
							displayText("[���� �Ϸ�: " + socketChannel.getRemoteAddress() + "]");
							btnConn.setText("stop");
							btnSend.setDisable(false);
						} catch (IOException e) {
						}
					});

				} catch (Exception e) {
					Platform.runLater(() -> displayText("[���� ��� �ȵ�]"));
					if (socketChannel.isOpen()) {
						stopClient();
					}
					return;

				}
				receive(); // �������� ���� ������ �ޱ�
			}
		};
		thread.start();
	}

	void stopClient() {
		// ���� ���� �ڵ�
		try {
			Platform.runLater(() -> {
				displayText("[�������]");
				btnConn.setText("start");
				btnSend.setDisable(true);
			});
			if (socketChannel != null && socketChannel.isOpen()) {
				socketChannel.close();
			}
		} catch (Exception e) {
		}
	}

	void receive() {
		// ������ �ޱ� �ڵ�
		while (true) {
			try {
				ByteBuffer byteBuffer = ByteBuffer.allocate(100); // �ʹ��̷�Ʈ ���� ����

				// ������ ������������ �������� ��� IOException �߻�
				int readByteCount = socketChannel.read(byteBuffer); // �����͸� ������ �迭�� ���� �� ��������

				// ������ ���������� Socket�� close()�� ȣ������ ���
				if (readByteCount == -1) {
					throw new IOException();
				}

				byteBuffer.flip(); // ������ 0��ġ
				Charset charset = Charset.forName("UTF-8");
				String data = charset.decode(byteBuffer).toString(); // ���ڿ��� ��ȯ

				Platform.runLater(() -> displayText("[�ޱ� �Ϸ�] " + data));

			} catch (Exception e) {
				Platform.runLater(() -> displayText("[������� �ȵ�]"));
				stopClient();
				break;
			}
		}
	}

	void send(String data) {
		// ������ ���� �ڵ�
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					Charset charset = Charset.forName("UTF-8");
					ByteBuffer byteBuffer = charset.encode(data);
					socketChannel.write(byteBuffer);
					Platform.runLater(() -> displayText("[������ �Ϸ�]"));

				} catch (Exception e) {
					Platform.runLater(() -> displayText("[������� �ȵ�]"));
					stopClient();
				}
			}
		};
		thread.start();
	}

	////////////////////////////////////////
	///// UI �����ڵ�

	TextArea txtDisplay;
	javafx.scene.control.TextField txtInput;
	Button btnConn, btnSend;

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		BorderPane root = new BorderPane();
		root.setPrefSize(500, 300);

		txtDisplay = new TextArea();
		txtDisplay.setEditable(false);
		BorderPane.setMargin(txtDisplay, new Insets(0, 0, 2, 0));
		root.setCenter(txtDisplay);

		BorderPane bottom = new BorderPane();
		txtInput = new javafx.scene.control.TextField();
		txtInput.setPrefSize(60, 30);
		root.setCenter(txtDisplay);

		btnConn = new Button("start");
		btnConn.setPrefSize(60, 30);
		btnConn.setOnAction(e -> {
			if (btnConn.getText().equals("start")) {
				startClient();
			} else if (btnConn.getText().equals("stop")) {
				stopClient();
			}
		});

		btnSend = new Button("send");
		btnSend.setPrefSize(60, 30);
		btnSend.setDisable(true);
		btnSend.setOnAction(e -> send(txtInput.getText()));

		bottom.setCenter(txtInput);
		bottom.setLeft(btnConn);
		bottom.setRight(btnSend);
		root.setBottom(bottom);

		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Client");
		primaryStage.setOnCloseRequest(event -> stopClient());
		primaryStage.show();
	}

	void displayText(String text) {
		txtDisplay.appendText(text + "\n");
	}

}
