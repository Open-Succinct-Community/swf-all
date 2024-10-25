package com.venky.swf.integration.api; 

public enum HttpMethod {
    GET() {
        public String toString() {
            return "GET";
        }
    },
    POST(){
        public String toString() {
            return "POST";
        }
    },
    PUT() {
        public String  toString() {
            return "PUT" ;
        }
    },
    DELETE() {
        public String  toString() {
            return "DELETE" ;
        }
    },

}
