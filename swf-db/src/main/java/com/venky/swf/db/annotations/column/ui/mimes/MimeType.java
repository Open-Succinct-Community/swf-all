package com.venky.swf.db.annotations.column.ui.mimes;

import com.venky.core.util.ObjectUtil;

import java.util.HashMap;
import java.util.Map;

public enum MimeType {
	TEXT_MARKDOWN() {
		public String toString(){
			return "text/x-web-markdown";
		}

		@Override
		public boolean isImage() {
			return false;
		}
	},
	TEXT_CSV() {
		public String toString(){
			return "text/csv";
		}
		public boolean isImage(){
			return false;
		}
	},
	TEXT_PLAIN(){
		public String toString(){
			return "text/plain";
		}
		public boolean isImage(){
			return false;
		}
	},
	TEXT_HTML (){
		public String toString(){
			return "text/html";
		}
		public boolean isImage(){
			return false;
		}
	},
	IMAGE_JPEG(){
		public String toString(){
			return "image/jpeg";
		}
		public boolean isImage(){
			return true;
		}
	},
	IMAGE_GIF(){
		public String toString(){
			return "image/gif";
		}
		public boolean isImage(){
			return true;
		}
	},
	IMAGE_PNG(){
		public String toString(){
			return "image/png";
		}
		public boolean isImage(){
			return true;
		}
	},
	/*
	TEXT_XML(){
		public String toString(){
			return "text/xml";
		}
		public boolean isImage(){
			return false;
		}
	},*/
	APPLICATION_XML(){
		public String toString(){
			return "application/xml";
		}
		public boolean isImage(){
			return false;
		}
	},
	APPLICATION_JSON(){
		public String toString(){
			return "application/json";
		}
		public boolean isImage(){
			return false;
		}
	},
	APPLICATION_XLS() {
		public String toString(){
			return "application/vnd.ms-excel";
		}
		public boolean isImage(){
			return false;
		}
	},
	APPLICATION_ZIP() {
		public String toString(){
			return "application/zip";
		}
		public boolean isImage(){
			return false;
		}
	},
	APPLICATION_OCTET_STREAM() {
		public String toString(){
			return "application/octet-stream";
		}
		public boolean isImage(){
			return false;
		}
	},
    APPLICATION_PDF() {
        public String toString(){
            return "application/pdf";
        }
        public boolean isImage(){
            return false;
        }
    },
	;

	public abstract boolean isImage();


	private static Map<String,MimeType> mimeTypeMap = new HashMap<>();
	private static void loadMimeTypeMap(){
		if (mimeTypeMap.isEmpty()){
			synchronized (mimeTypeMap){
				if  (mimeTypeMap.isEmpty()){
					return;
				}
				for (MimeType type : MimeType.values()){
					mimeTypeMap.put(type.toString(),type);
				}
			}
		}
	}

	public static MimeType getMimeType(String mimeType){
		loadMimeTypeMap();
		return mimeTypeMap.get(mimeType);
	}
}
