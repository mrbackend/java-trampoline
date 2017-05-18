# java-trampoline

![Build Status](https://travis-ci.org/mrbackend/java-trampoline.svg?branch=master)

## The problem with recursion

Consider the following recursive `List` type:
```java
public abstract class List<A> {

    public abstract int size();

    private static final class Nil<A> extends List<A> {
        @Override
        public int size() {
            return 0;
        }
    }

    private static final class Cons<A> extends List<A> {
        private final A head;
        private final List<A> tail;

        private Cons(A head, List<A> tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public int size() {
            return tail.size() + 1;
        }
    }

}
```

`int size()` is implemented using recursion. However, for large `List`s, running `size()` will overflow the stack.

## How it works

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut commodo accumsan nunc, ac feugiat tellus. Fusce quis 
ullamcorper quam. Ut venenatis turpis dolor, vitae pretium tortor feugiat vel. Duis vitae pharetra felis. Mauris
efficitur ante et ipsum dictum aliquam. Etiam lobortis tempus tortor, sit amet vehicula ipsum volutpat at. Nulla maximus
finibus dui vitae maximus. Suspendisse mattis dui non neque ullamcorper egestas eu at elit. Mauris tempus gravida
sodales. Maecenas dapibus laoreet leo, non luctus turpis semper eu. Fusce mauris turpis, faucibus sed porta
pellentesque, volutpat consectetur odio. Aliquam id purus efficitur, semper metus id, porttitor nulla. Curabitur ac
sodales nibh. Aenean sed pellentesque lectus. Pellentesque ullamcorper eu sapien sit amet lobortis. Curabitur dolor
tellus, porta ac enim quis, tempus vestibulum ante.

![resume(FlatMap(FlatMap))](https://rawgit.com/mrbackend/java-trampoline/master/svg/resume-flatmap-flatmap.svg)

Cras sollicitudin porta justo, in ultricies ligula tempor ac. Vestibulum suscipit tincidunt auctor. Cras vitae mattis
dolor. Aenean nec nulla vel felis scelerisque eleifend sed vitae augue. In tempus, tellus id eleifend tincidunt, libero
turpis aliquam felis, ut tincidunt quam purus vitae orci. Vivamus eget tempus tortor, et ultrices felis. Praesent a
turpis enim. Donec tempor commodo tellus, a accumsan metus lobortis in. Donec bibendum enim eget nunc aliquet
efficitur. Donec massa risus, pharetra eu vulputate nec, fermentum et purus. Vestibulum aliquet arcu elit, sed
fermentum est tempor vel. Duis et placerat sem.