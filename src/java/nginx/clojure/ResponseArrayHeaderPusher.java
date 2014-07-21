/**
 *  Copyright (C) Zhang,Yuexiang (xfeep)
 *
 */
package nginx.clojure;

import static nginx.clojure.MiniConstants.DEFAULT_ENCODING;
import static nginx.clojure.MiniConstants.NGX_HTTP_CLOJURE_ARRAY_ELTS_OFFSET;
import static nginx.clojure.MiniConstants.NGX_HTTP_CLOJURE_HEADERSO_HEADERS_OFFSET;
import static nginx.clojure.MiniConstants.NGX_HTTP_CLOJURE_TELT_SIZE;
import static nginx.clojure.MiniConstants.NGX_HTTP_CLOJURE_TEL_HASH_OFFSET;
import static nginx.clojure.MiniConstants.NGX_HTTP_CLOJURE_TEL_KEY_OFFSET;
import static nginx.clojure.MiniConstants.NGX_HTTP_CLOJURE_TEL_VALUE_OFFSET;
import static nginx.clojure.MiniConstants.NGX_OK;
import static nginx.clojure.NginxClojureRT.UNSAFE;

import java.util.Arrays;
import java.util.List;



public class ResponseArrayHeaderPusher implements ResponseHeaderPusher {

	protected long offset;
	protected String name;
	
	public ResponseArrayHeaderPusher(String name, long offset) {
		this.offset = offset;
		this.name = name;
	}
	
	@Override
	public long knownOffset() {
		return offset;
	}

	@Override
	public String name() {
		return name;
	}
	
	@Override
	public void push(long h, long pool, Object v) {
		long haddr = h + offset;
		if (haddr == 0){
			throw new RuntimeException("invalid address for set header array value " + v);
		}
		
		List<String> seq = null;
		if (v == null || v instanceof String) {
			String val = (String) v;
			seq = Arrays.asList(val);
		}else if (v instanceof List) {
			seq = (List) v;
		}else if (v.getClass().isArray()){
			seq = (List)Arrays.asList((Object[])v);
		}
		
		int c = seq.size();
		if (c == 0) {
			return;
		}
		
		long lp = UNSAFE.getAddress(haddr + NGX_HTTP_CLOJURE_ARRAY_ELTS_OFFSET);
		if (lp == 0) {
			long code = NginxClojureRT.ngx_array_init(haddr, pool, c, NGX_HTTP_CLOJURE_TELT_SIZE);
			if (code != NGX_OK) {
				throw new RuntimeException("can not init ngx array for header, return code:" + code);
			}
		}
		
		lp = NginxClojureRT.ngx_array_push_n(haddr, c);
		if (lp == 0) {
			throw new RuntimeException("can not push ngx array for header");
		}
		
		for (String val : seq) {
			if (val != null) {
				long p = NginxClojureRT.ngx_list_push(h + NGX_HTTP_CLOJURE_HEADERSO_HEADERS_OFFSET);
				if (p == 0) {
					throw new RuntimeException("can not push ngx list for headers");
				}
				NginxClojureRT.pushNGXInt(p + NGX_HTTP_CLOJURE_TEL_HASH_OFFSET, 1);
				NginxClojureRT.pushNGXString(p + NGX_HTTP_CLOJURE_TEL_KEY_OFFSET, name, DEFAULT_ENCODING, pool);
				NginxClojureRT.pushNGXString(p + NGX_HTTP_CLOJURE_TEL_VALUE_OFFSET, val, DEFAULT_ENCODING, pool);
				UNSAFE.putAddress(lp, p);
				lp += NGX_HTTP_CLOJURE_TELT_SIZE;
			}
		}
	}



}
