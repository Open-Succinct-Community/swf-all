package com.venky.swf.plugins.background.core;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.venky.core.util.MultiException;
import de.javakaffee.kryoserializers.JdkProxySerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;

public class SerializationHelper {
    Kryo kryo = null;
    public SerializationHelper (){
        kryo = createCryo();
    }
    private Kryo createCryo(){
        Kryo kryo = new Kryo();
        kryo.setClassLoader(this.getClass().getClassLoader());
        kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
        kryo.register(InvocationHandler.class, new JdkProxySerializer());
        return kryo;
    }
    public <T> void write(OutputStream os, T object){
        Output output = new Output(os);
        kryo.writeClassAndObject(output,object);
        output.flush();
    }
    public <T> T read(InputStream in){
        MultiException multiException = new MultiException("Read Failed");
        try {
            in.mark(in.available());
            Input input = new Input(in);
            return (T)kryo.readClassAndObject(input);
        }catch (Exception ex){
            multiException.add(ex);
        }finally {
            try {
                in.reset();
            }catch (IOException ex){
                multiException.add(new RuntimeException(ex));
            }
        }
        //Back wards Compatibility
        try {
            in.mark(in.available());
            ObjectInputStream is = new ObjectInputStream(in);
            return (T)is.readObject();
        }catch (Exception ex){
            multiException.add(ex);
        }finally {
            try {
                in.reset();
            }catch (IOException ex){
                multiException.add(new RuntimeException(ex));
            }
        }
        throw multiException;
    }
}
