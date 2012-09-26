package com.twelvemonkeys.lang;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

/**
 * “If it walks like a duck, looks like a duck, quacks like a duck, it must be…”
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/test/java/com/twelvemonkeys/lang/DuckTypeTestCase.java#1 $
 */
public class DuckTypeTestCase extends TestCase {

    static public interface Eatable {
    }

    static public interface Vegetable extends Eatable {
    }

    static public interface Meat extends Eatable {
    }

    static public interface Animal {
        void walk();
        boolean canEat(Eatable pFood);
        void eat(Eatable pFood);
    }

    static public interface Bird extends Animal {
        void fly();
    }

    static public interface Duck extends Bird {
        void quack();
    }

    static public class DuckLookALike {
        private boolean mWalking;
        private boolean mFlying;
        private boolean mQuacking;

        public void walk() {
            mWalking = true;
        }

        public void fly() {
            mFlying = true;
        }

        public void quack() {
            mQuacking = true;
        }

        void reset() {
            mWalking = mFlying = mQuacking = false;
        }

        boolean verify() {
            return mWalking && mFlying && mQuacking;
        }
    }

    static public class Swan extends DuckLookALike {
    }

    static public class VeggieEater {
        private boolean mHappy;

        public boolean canEat(Eatable pFood) {
            return pFood instanceof Vegetable;
        }

        public void eat(Eatable pFood) {
            if (pFood == this) {
                throw new IllegalArgumentException("CantEatMyselfException: duh");
            }
            if (!canEat(pFood)) {
                throw new NotVegetableException("yuck");
            }

            mHappy = true;
        }

        void reset() {
            mHappy = false;
        }

        boolean verify() {
            return mHappy;
        }
    }

    static class NotVegetableException extends RuntimeException {
        public NotVegetableException(String message) {
            super(message);
        }
    }

    public static void testTooManyThingsAtOnce() {
        DuckLookALike lookALike = new DuckLookALike();
        VeggieEater veggieEater = new VeggieEater();

        Object obj = DuckType.implement(new Class[]{Duck.class, Meat.class},
                                        new Object[]{lookALike, veggieEater});
        assertTrue(obj instanceof Duck);
        assertTrue(obj instanceof Meat);
        Duck duck = (Duck) obj;

        Bird another = (Bird) DuckType.implement(new Class[]{Duck.class, Meat.class},
                                                 new Object[]{lookALike, veggieEater});

        Duck uglyDuckling = (Duck) DuckType.implement(new Class[] {Duck.class, Meat.class},
                                                      new Object[] {new Swan(), new VeggieEater()});

        assertNotNull(duck.toString());

        assertTrue("Duck is supposed to equal itself (identity crisis)", duck.equals(duck));

        assertEquals("Duck is supposed to equal other duck with same stuffing", duck, another);

        assertFalse("Some ducks are more equal than others", duck.equals(uglyDuckling));

        duck.walk();
        duck.quack();
        duck.quack();
        duck.fly();

        assertTrue("Duck is supposed to quack", lookALike.verify());

        Vegetable cabbage = new Vegetable() {};
        assertTrue("Duck is supposed to like cabbage", duck.canEat(cabbage));
        duck.eat(cabbage);
        assertTrue("Duck is supposed to eat vegetables", veggieEater.verify());

        veggieEater.reset();

        Throwable exception = null;
        try {
            duck.eat((Meat) uglyDuckling);
            fail("Duck ate distant cousin");
        }
        catch (AssertionFailedError e) {
            throw e;
        }
        catch (Throwable t) {
            exception = t;
        }
        assertTrue("Incorrect quack: " + exception, exception instanceof NotVegetableException);


        // TODO: There's a flaw in the design here..
        // The "this" keyword don't work well with proxies..

        // Something that could possibly work, is:
        // All proxy-aware delegates need a method getThis() / getSelf()...
        // (using a field won't work for delegates that are part of multiple
        // proxies).
        // The default implementation should only return "this"..
        // TBD: How do we know which proxy the current delegate is part of? 

        exception = null;
        try {
            duck.eat((Meat) duck);
            fail("Duck ate itself");
        }
        catch (AssertionFailedError e) {
            throw e;
        }
        catch (Throwable t) {
            exception = t;
        }
        assertTrue("Duck tried to eat itself: " + exception, exception instanceof IllegalArgumentException);
    }

    public void testExpandedArgs() {
        Object walker = new Object() {
            public void walk() {
            }
        };
        Object eater = new Object() {
            // Assignable, but not direct match
            public boolean canEat(Object pFood) {
                return true;
            }

            // Assignable, but not direct match
            public void eat(Object pFood) {
            }
        };

        Animal rat = (Animal) DuckType.implement(new Class[]{Animal.class, Meat.class},
                                                 new Object[]{walker, eater});

        assertNotNull(rat);
        assertTrue(rat instanceof Meat);

        // Rats eat everything
        Eatable smellyFood = new Eatable() {boolean tastesVeryBad = true;};
        assertTrue("Rat did not eat smelly food", rat.canEat(smellyFood));
    }

    public void testExpandedArgsFail() {
        try {
            Object walker = new Object() {
                public void walk() {
                }
            };
            Object eater = new Object() {
                // Not assignable return type
                public int canEat(Eatable pFood) {
                    return 1;
                }

                // Assignable, but not direct match
                public void eat(Object pFood) {
                }
            };
            DuckType.implement(new Class[]{Animal.class},
                               new Object[]{walker, eater});

            fail("This kind of animal won't live long");
        }
        catch (DuckType.NoMatchingMethodException e) {
        }
    }

    public void testStubAbstract() {
        Object obj = DuckType.implement(new Class[]{Animal.class},
                                        new Object[]{new Object()}, true);
        assertTrue(obj instanceof Animal);
        Animal unicorn = (Animal) obj;
        assertNotNull(unicorn);

        // Should create a meaningful string representation
        assertNotNull(unicorn.toString());

        // Unicorns don't fly, as they are only an abstract idea..
        try {
            unicorn.walk();
            fail("Unicorns should not fly, as they are only an abstract idea");
        }
        catch (AbstractMethodError e) {
        }
    }
}
