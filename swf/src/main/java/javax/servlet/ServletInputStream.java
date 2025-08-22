package javax.servlet;

import jakarta.servlet.ReadListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ServletInputStream  extends jakarta.servlet.ServletInputStream {
    public ServletInputStream(){

    }

    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        return inputStream.readLine(b, off, len);
    }

    @Override
    public boolean isFinished() {
        return inputStream.isFinished();
    }

    @Override
    public boolean isReady() {
        return inputStream.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        inputStream.setReadListener(readListener);
    }

    public static InputStream nullInputStream() {
        return InputStream.nullInputStream();
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read( byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public int read( byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return inputStream.readAllBytes();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        return inputStream.readNBytes(len);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        return inputStream.readNBytes(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }


    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return inputStream.transferTo(out);
    }

    jakarta.servlet.ServletInputStream inputStream ;
    public ServletInputStream(jakarta.servlet.ServletInputStream inputStream){
        this.inputStream = inputStream;
    }


}
