package com.twelvemonkeys.imageio.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SequenceSupportTest {
    @Test
    void happyCase() {
        SequenceSupport sequence = new SequenceSupport();

        sequence.start();
        assertEquals(0, sequence.current());

        for (int i = 0; i < Byte.MAX_VALUE; i++) {
            assertEquals(i, sequence.advance());
            assertEquals(i + 1, sequence.current());
        }

        assertEquals(127, sequence.end());
        assertEquals(-1, sequence.current());
        assertThrows(IllegalStateException.class, sequence::advance);
    }

    @Test
    void reset() {
        SequenceSupport sequence = new SequenceSupport();
        sequence.reset();

        assertEquals(-1, sequence.current());
        assertThrows(IllegalStateException.class, sequence::end);

        sequence.start();
        sequence.reset();

        assertEquals(-1, sequence.current());
        assertThrows(IllegalStateException.class, sequence::end);

        sequence.start();
        sequence.advance();
        sequence.reset();

        assertEquals(-1, sequence.current());
        assertThrows(IllegalStateException.class, sequence::end);

        sequence.start();
        sequence.end();
        sequence.reset();

        assertEquals(-1, sequence.current());
        assertThrows(IllegalStateException.class, sequence::end);
    }

    @Test
    void startEnd() {
        SequenceSupport sequence = new SequenceSupport();
        sequence.start();
        sequence.end();

        assertEquals(-1, sequence.current());
        assertThrows(IllegalStateException.class, sequence::end);
    }

    @Test
    void startAlreadyStarted() {
        SequenceSupport sequence = new SequenceSupport();
        sequence.start();

        assertThrows(IllegalStateException.class, sequence::start);
    }

    @Test
    void advanceNotStarted() {
        SequenceSupport sequence = new SequenceSupport();
        assertThrows(IllegalStateException.class, sequence::advance);
    }

    @Test
    void currentNotStarted() {
        SequenceSupport sequence = new SequenceSupport();
        assertEquals(-1, sequence.current());
    }

    @Test
    void endNotStarted() {
        SequenceSupport sequence = new SequenceSupport();
        assertThrows(IllegalStateException.class, sequence::end);
    }
}