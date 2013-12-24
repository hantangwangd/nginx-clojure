package nginx.clojure;

import java.io.IOException;
import java.io.InputStream;

import static nginx.clojure.NginxClojureRT.*;
import static nginx.clojure.Constants.*;

public class NativeInputStream extends InputStream {

	protected long addr;
	protected long len;
	protected long pos;
	
	
	public NativeInputStream(long addr, long len) {
		super();
		this.addr = addr;
		this.len = len;
		this.pos = 0;
	}


	@Override
	public int read() throws IOException {
		if (pos < len) {
			return UNSAFE.getByte(addr + pos++) & 0xff;
		}
		return -1;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}
	
	@Override
	public int read(byte[] b, int off, int l) throws IOException {
		if (pos == len) {
			return -1;
		}
		int c = (int)(len - pos);
		if (l > b.length - off) {
			l = b.length - off;
		}
		if (c > l) {
			c = l;
		}
		ngx_http_clojure_mem_copy_to_obj(addr + pos, b, BYTE_ARRAY_OFFSET + off, c);
		pos += c;
		return c;
	}
}
