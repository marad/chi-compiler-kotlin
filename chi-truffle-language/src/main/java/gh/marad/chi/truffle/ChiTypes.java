package gh.marad.chi.truffle;

import com.oracle.truffle.api.dsl.TypeSystem;

import java.math.BigDecimal;

@TypeSystem({int.class, long.class, BigDecimal.class, String.class})
public class ChiTypes {
}
