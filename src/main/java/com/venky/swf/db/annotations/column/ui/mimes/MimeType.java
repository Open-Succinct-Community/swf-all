package com.venky.swf.db.annotations.column.ui.mimes;

public enum MimeType {
	
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
	TEXT_XML(){
		public String toString(){
			return "text/xml";
		}
		public boolean isImage(){
			return false;
		}
	}
	;

	public abstract boolean isImage();
}
