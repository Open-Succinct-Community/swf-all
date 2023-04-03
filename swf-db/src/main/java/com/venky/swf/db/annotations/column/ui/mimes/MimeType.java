package com.venky.swf.db.annotations.column.ui.mimes;

import java.util.HashMap;
import java.util.Map;

public enum MimeType {
	TEXT_JAVASCRIPT() {
		public String toString(){
			return "text/javascript";
		}
		@Override
		public boolean isImage() {
			return false;
		}

		@Override
		public String getDefaultFileExtension() {
			return "js";
		}
	},
	TEXT_CSS(){
		public String toString(){
			return "text/css";
		}
		@Override
		public boolean isImage() {
			return false;
		}

		@Override
		public String getDefaultFileExtension() {
			return "css";
		}
	},
	TEXT_MARKDOWN() {
		public String toString(){
			return "text/x-web-markdown";
		}

		@Override
		public boolean isImage() {
			return false;
		}

		@Override
		public String getDefaultFileExtension() {
			return "md";
		}
	},
	TEXT_CSV() {
		public String toString(){
			return "text/csv";
		}
		public boolean isImage(){
			return false;
		}
		@Override
		public String getDefaultFileExtension() {
			return "csv";
		}
	},
	TEXT_PLAIN(){
		public String toString(){
			return "text/plain";
		}
		public boolean isImage(){
			return false;
		}
		@Override
		public String getDefaultFileExtension() {
			return "txt";
		}

	},
	TEXT_HTML (){
		public String toString(){
			return "text/html";
		}
		public boolean isImage(){
			return false;
		}
		@Override
		public String getDefaultFileExtension() {
			return "html";
		}

	},
	IMAGE_JPEG(){
		public String toString(){
			return "image/jpeg";
		}
		public boolean isImage(){
			return true;
		}
		@Override
		public String getDefaultFileExtension() {
			return "jpg";
		}
	},
	IMAGE_GIF(){
		public String toString(){
			return "image/gif";
		}
		public boolean isImage(){
			return true;
		}
		@Override
		public String getDefaultFileExtension() {
			return "gif";
		}
	},
	IMAGE_PNG(){
		public String toString(){
			return "image/png";
		}
		public boolean isImage(){
			return true;
		}
		@Override
		public String getDefaultFileExtension() {
			return "png";
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
		@Override
		public String getDefaultFileExtension() {
			return "xml";
		}

	},
	APPLICATION_JSON(){
		public String toString(){
			return "application/json";
		}
		public boolean isImage(){
			return false;
		}
		@Override
		public String getDefaultFileExtension() {
			return "json";
		}

	},
	APPLICATION_XLS() {
		public String toString(){
			return "application/vnd.ms-excel";
		}
		public boolean isImage(){
			return false;
		}
		public boolean isText() {
			return false;
		}
		@Override
		public String getDefaultFileExtension() {
			return "xls";
		}

	},
	APPLICATION_XLSX() {
		public String toString(){
			return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		}
		public boolean isImage(){
			return false;
		}
		public boolean isText() {
			return false;
		}
		@Override
		public String getDefaultFileExtension() {
			return "xlsx";
		}

	},
	APPLICATION_ZIP() {
		public String toString(){
			return "application/zip";
		}
		public boolean isImage(){
			return false;
		}
		public boolean isText() {
			return false;
		}
		@Override
		public String getDefaultFileExtension() {
			return "zip";
		}

	},
	APPLICATION_OCTET_STREAM() {
		public String toString(){
			return "application/octet-stream";
		}
		public boolean isImage(){
			return false;
		}
		public boolean isText() {
			return false;
		}
		@Override
		public String getDefaultFileExtension() {
			return "bin";
		}

	},
    APPLICATION_PDF() {
        public String toString(){
            return "application/pdf";
        }
        public boolean isImage(){
            return false;
        }
		public boolean isText() {
			return false;
		}
		@Override
		public String getDefaultFileExtension() {
			return "pdf";
		}

	},
	APPLICATION_GRAPHQL() {
		public String toString(){
			return "application/graphql";
		}
		public boolean isImage(){
			return false;
		}
		@Override
		public String getDefaultFileExtension() {
			return "gql";
		}
	}
	;

	public abstract boolean isImage();
	public boolean isText() {
		return !isImage();
	}
	public abstract String getDefaultFileExtension();


	private static Map<String,MimeType> mimeTypeMap = new HashMap<String,MimeType>(){{
		for (MimeType type : MimeType.values()) {
			put(type.toString(), type);
		}
	}};

	public static MimeType getMimeType(String mimeType){
		return mimeTypeMap.get(mimeType);
	}
}
