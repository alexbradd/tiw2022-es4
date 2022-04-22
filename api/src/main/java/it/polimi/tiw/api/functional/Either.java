package it.polimi.tiw.api.functional;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Either<L, R> {

    public static <L, R> Either<L, R> left(L left) {
        return new Left<>(left);
    }

    public static <L, R> Either<L, R> right(R right) {
        return new Right<>(right);
    }

    private static <U, V> Either<U, V> cast(Either<? extends U, ? extends V> either) {
        return either.match(
                (Function<U, Either<U, V>>) Either::left,
                (Function<V, Either<U, V>>) Either::right);
    }

    public boolean isLeft() {
        return fold(l -> true, r -> false);
    }

    public boolean isRight() {
        return !isLeft();
    }

    public <U> U fold(Function<? super L, ? extends U> leftFolder, Function<? super R, ? extends U> rightFolder) {
        Objects.requireNonNull(rightFolder);
        Objects.requireNonNull(leftFolder);
        return match(leftFolder, rightFolder);
    }

    public <U> Either<L, U> flatMap(Function<? super R, ? extends Either<L, ? extends U>> mapper) {
        Objects.requireNonNull(mapper);
        return match(Either::left, (R r) -> cast(mapper.apply(r)));
    }

    public <U, V> Either<U, V> flatMap(Function<? super L, ? extends Either<? extends U, ? extends V>> leftMapper,
                                       Function<? super R, ? extends Either<? extends U, ? extends V>> rightMapper) {
        Objects.requireNonNull(rightMapper);
        Objects.requireNonNull(leftMapper);
        return match(
                (L l) -> cast(leftMapper.apply(l)), (R r) -> cast(rightMapper.apply(r))
        );
    }

    public <U> Either<L, U> map(Function<? super R, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return match(Either::left, (R r) -> right(mapper.apply(r)));
    }

    public <U, V> Either<U, V> map(Function<? super L, ? extends U> leftMapper,
                                   Function<? super R, ? extends V> rightMapper) {
        Objects.requireNonNull(rightMapper);
        Objects.requireNonNull(leftMapper);
        return match(
                (L l) -> left(leftMapper.apply(l)), (R r) -> right(rightMapper.apply(r))
        );
    }

    public abstract <E> E match(Function<? super L, ? extends E> left, Function<? super R, ? extends E> right);

    public abstract void consume(Consumer<? super L> left, Consumer<? super R> right);

    public L fromLeft(L def) {
        return match(l -> l, r -> def);
    }

    public R fromRight(R def) {
        return match(l -> def, r -> r);
    }

    private static class Left<L, R> extends Either<L, R> {
        private final L left;

        private Left(L left) {
            this.left = left;
        }

        @Override
        public <E> E match(Function<? super L, ? extends E> left, Function<? super R, ? extends E> right) {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            return left.apply(this.left);
        }

        @Override
        public void consume(Consumer<? super L> left, Consumer<? super R> right) {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            left.accept(this.left);
        }
    }

    private static class Right<L, R> extends Either<L, R> {
        private final R right;

        private Right(R right) {
            this.right = right;
        }

        @Override
        public <E> E match(Function<? super L, ? extends E> left, Function<? super R, ? extends E> right) {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            return right.apply(this.right);
        }

        @Override
        public void consume(Consumer<? super L> left, Consumer<? super R> right) {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            right.accept(this.right);
        }
    }
}
