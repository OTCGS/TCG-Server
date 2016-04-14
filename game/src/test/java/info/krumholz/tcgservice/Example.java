package info.krumholz.tcgservice;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class Example {

	public static void main(String[] args) throws IOException {
		Path tempFile = Files.createTempFile("foo", "bar");
		OutputStream newOutputStream = Files.newOutputStream(tempFile, StandardOpenOption.WRITE);
		new Thread(() -> {
			try {
				while (true) {
					newOutputStream.write(new byte[] { 1, 2, 3, 4, 5, 6 });
					Thread.sleep(2000);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();

		while (true) {
			byte[] readAllBytes = Files.readAllBytes(tempFile);
			System.out.println(Arrays.toString(readAllBytes));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
