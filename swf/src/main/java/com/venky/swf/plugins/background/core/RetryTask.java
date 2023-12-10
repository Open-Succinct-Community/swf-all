package com.venky.swf.plugins.background.core;

public abstract class RetryTask implements Task{

    Class<? extends Throwable>[] throwableClasses;
    @SafeVarargs
    public final RetryTask retryOn(Class<? extends Throwable>... throwableClasses){
        this.throwableClasses = throwableClasses;
        return this;
    }

    @Override
    public void onException(Throwable currentException) {
        Task.super.onException(currentException);
        if (isRetryNecessary(currentException)) {
            Task.super.onStart();
            execute();
            Task.super.onSuccess();
        }
    }
    public boolean isRetryNecessary(Throwable ex){
        if (this.throwableClasses == null ){
            return true;
        }
        for (Class<? extends Throwable> throwableClass : this.throwableClasses) {
            if (throwableClass.isInstance(ex)) {
                return true;
            }
        }
        return false;
    }

}
