package com.grinderwolf.swm.nms.errorables;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/*
I hate checked exceptions
 */
@SuppressWarnings("unchecked")
public class ErrorableCompletableFuture<T> extends CompletableFuture<T> {

    public ErrorableCompletableFuture<Void> thenAccept(ErrorableConsumer<? super T> action) {
        return (ErrorableCompletableFuture<Void>) super.thenAccept(action);
    }

    public ErrorableCompletableFuture<Void> thenRun(ErrorableRunnable action) {
        return (ErrorableCompletableFuture<Void>) super.thenRun(action);
    }

    public <U> ErrorableCompletableFuture<U> thenApply(ErrorableFunction<? super T, ? extends U> fn) {
        return (ErrorableCompletableFuture<U>) super.thenApply(fn);
    }

    public <U> ErrorableCompletableFuture<U> thenCompose(ErrorableFunction<? super T, ? extends CompletionStage<U>> fn) {
        return (ErrorableCompletableFuture<U>) super.thenCompose(fn);
    }

    public <U, V> ErrorableCompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return (ErrorableCompletableFuture<V>) super.thenCombine(other, fn);
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new ErrorableCompletableFuture<>();
    }

    public interface ErrorableConsumer<T> extends Consumer<T> {

        void acceptExceptionally(T t) throws Exception;

        @Override
        default void accept(T t) {
            try {
                acceptExceptionally(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public interface ErrorableRunnable extends Runnable {

        void runExceptionally() throws Exception;

        @Override
        default void run() {
            try {
                runExceptionally();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public interface ErrorableFunction<T, R> extends Function<T, R> {

        R exceptionallyApply(T t) throws Exception;

        @Override
        default R apply(T t) {
            try {
                return exceptionallyApply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


}
