package dinkplugin.message.templating;

import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.function.Supplier;

@RequiredArgsConstructor
public final class LazyEvaluable implements Evaluable {
    private final Supplier<Evaluable> supplier;
    private Evaluable resolved = null;

    @Override
    public String evaluate(boolean rich) {
        return this.resolve().evaluate(rich);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Evaluable)) return false;
        LazyEvaluable that = (LazyEvaluable) o;
        return Objects.equals(this.resolve(), that.resolve());
    }

    @Override
    public int hashCode() {
        return this.resolve().hashCode();
    }

    private Evaluable resolve() {
        Evaluable e = this.resolved;
        if (e != null) return e;
        return this.resolved = this.supplier.get();
    }

    public static LazyEvaluable of(Supplier<Evaluable> supplier) {
        return new LazyEvaluable(supplier);
    }
}
