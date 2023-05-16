/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.internal.stream.tokenization;

import java.util.Objects;

/*
 * Processing states.
 */
public enum State {

    // Invalid States
    INVALID(DialectCode.UNKNOWN, Category.INVALID),
    INVALID_TAG_LENGTH(DialectCode.UNKNOWN, Category.INVALID),

    // Initial States
    INITIAL(DialectCode.UNKNOWN, Category.INITIAL),
    INTERCHANGE_END(DialectCode.UNKNOWN, Category.INITIAL),
    HEADER_EDIFACT_U(DialectCode.UNKNOWN, Category.EDIFACT_1),
    HEADER_EDIFACT_N(DialectCode.UNKNOWN, Category.EDIFACT_2),
    HEADER_TRADACOMS_S(DialectCode.UNKNOWN, Category.TRADACOMS_1),
    HEADER_TRADACOMS_T(DialectCode.UNKNOWN, Category.TRADACOMS_2),
    HEADER_X12_I(DialectCode.UNKNOWN, Category.X12_1),
    HEADER_X12_S(DialectCode.UNKNOWN, Category.X12_2),

    // Common States (shared among dialects)
    INTERCHANGE_CANDIDATE(Category.HEADER), // IC
    HEADER_DATA(Category.HEADER), // HD
    HEADER_SEGMENT_BEGIN(Category.HEADER),
    HEADER_INVALID_DATA(Category.HEADER), // HV
    HEADER_COMPONENT_END(Category.HEADER), // HC
    HEADER_ELEMENT_END(Category.HEADER), // HE
    HEADER_SEGMENT_END(Category.HEADER),
    HEADER_RELEASE(Category.HEADER_RELEASE), // HR
    TAG_SEARCH(Category.TAG_SEARCH),
    SEGMENT_END(Category.TAG_SEARCH),
    SEGMENT_EMPTY(Category.TAG_SEARCH),
    TAG_1(Category.TAG_1),
    TAG_2(Category.TAG_2),
    TAG_3(Category.TAG_3),
    SEGMENT_BEGIN(Category.ELEMENT_PROCESS),
    ELEMENT_DATA(Category.ELEMENT_PROCESS),
    ELEMENT_INVALID_DATA(Category.ELEMENT_PROCESS),
    COMPONENT_END(Category.ELEMENT_PROCESS),
    ELEMENT_REPEAT(Category.ELEMENT_PROCESS),
    ELEMENT_END(Category.ELEMENT_PROCESS),
    DATA_RELEASE(Category.DATA_RELEASE),
    ELEMENT_DATA_BINARY(Category.DATA_BINARY),
    ELEMENT_END_BINARY(Category.DATA_BINARY_END),
    TRAILER_BEGIN(Category.TRAILER),
    TRAILER_ELEMENT_DATA(Category.TRAILER),
    TRAILER_ELEMENT_END(Category.TRAILER),

    // EDIFACT
    TRAILER_EDIFACT_U(Category.TERM_7),
    TRAILER_EDIFACT_N(Category.TERM_8),
    TRAILER_EDIFACT_Z(Category.TERM_9),
    HEADER_EDIFACT_UNB_SEARCH(Category.EDIFACT_UNB_0), // EDIFACT UNA -> UNB Only
    HEADER_EDIFACT_UNB_1(Category.EDIFACT_UNB_1),      // EDIFACT UNA -> UNB Only
    HEADER_EDIFACT_UNB_2(Category.EDIFACT_UNB_2),      // EDIFACT UNA -> UNB Only
    HEADER_EDIFACT_UNB_3(Category.EDIFACT_UNB_3),      // EDIFACT UNA -> UNB Only

    // TRADACOMS
    TRAILER_TRADACOMS_E(Category.TERM_7),
    TRAILER_TRADACOMS_N(Category.TERM_8),
    TRAILER_TRADACOMS_D(Category.TERM_9),

    // X12
    TRAILER_X12_I(Category.TERM_7),
    TRAILER_X12_E(Category.TERM_8),
    TRAILER_X12_A(Category.TERM_9);

    public static final class DialectCode {
        private DialectCode() {}
        public static final int UNKNOWN   = 0;
        public static final int EDIFACT   = 1;
        public static final int TRADACOMS = 2;
        public static final int X12       = 3;
    }

    private static final class Category {
        // Initial
        static final int INVALID            = -1;
        static final int INITIAL            = 0;
        static final int EDIFACT_1          = 1;
        static final int EDIFACT_2          = 2;
        static final int TRADACOMS_1        = 3;
        static final int TRADACOMS_2        = 4;
        static final int X12_1              = 5;
        static final int X12_2              = 6;

        // Common (placed in dialect-specific tables)
        static final int HEADER             = 0;
        static final int HEADER_RELEASE     = 1;
        static final int TAG_1              = 2;
        static final int TAG_2              = 3;  // Common for EDIFACT & X12, overridden TRADACOMS
        static final int TAG_3              = 4;  // Common for EDIFACT & X12, overridden TRADACOMS
        static final int ELEMENT_PROCESS    = 5;
        static final int DATA_RELEASE       = 6;
        static final int DATA_BINARY        = 7;
        static final int DATA_BINARY_END    = 8;
        static final int TRAILER            = 9;
        // Dialect-Specific
        static final int TAG_SEARCH         = 10; // Each dialect has their own version to support transition to interchange end segments
        static final int TERM_7             = 11; // EDIFACT Unz, TRADACOMS End, X12 Iea
        static final int TERM_8             = 12; // EDIFACT uNz, TRADACOMS eNd, X12 iEa
        static final int TERM_9             = 13; // EDIFACT unZ, TRADACOMS enD, X12 ieA
        static final int EDIFACT_UNB_0      = 14;
        static final int EDIFACT_UNB_1      = 15;
        static final int EDIFACT_UNB_2      = 16;
        static final int EDIFACT_UNB_3      = 17;
    }

    private static final State __ = State.INVALID;
    private static final State _1 = State.INVALID_TAG_LENGTH;

    private static final State II = State.INITIAL;

    private static final State X1 = State.HEADER_X12_I;
    private static final State X2 = State.HEADER_X12_S;
    private static final State X7 = State.TRAILER_X12_I;
    private static final State X8 = State.TRAILER_X12_E;
    private static final State X9 = State.TRAILER_X12_A;

    private static final State U1 = State.HEADER_EDIFACT_U;
    private static final State U2 = State.HEADER_EDIFACT_N;
    private static final State U7 = State.TRAILER_EDIFACT_U;
    private static final State U8 = State.TRAILER_EDIFACT_N;
    private static final State U9 = State.TRAILER_EDIFACT_Z;

    private static final State C1 = State.HEADER_TRADACOMS_S;
    private static final State C2 = State.HEADER_TRADACOMS_T;
    private static final State C7 = State.TRAILER_TRADACOMS_E;
    private static final State C8 = State.TRAILER_TRADACOMS_N;
    private static final State C9 = State.TRAILER_TRADACOMS_D;

    private static final State IC = State.INTERCHANGE_CANDIDATE;

    private static final State HD = State.HEADER_DATA;
    private static final State HR = State.HEADER_RELEASE;
    private static final State HV = State.HEADER_INVALID_DATA;
    private static final State HC = State.HEADER_COMPONENT_END;
    private static final State HE = State.HEADER_ELEMENT_END;
    private static final State HZ = State.HEADER_SEGMENT_END;

//    private static final State B0 = State.HEADER_EDIFACT_UNB_SEARCH;
//    private static final State B1 = State.HEADER_EDIFACT_UNB_1;
//    private static final State B2 = State.HEADER_EDIFACT_UNB_2;
//    private static final State B3 = State.HEADER_EDIFACT_UNB_3;
//    private static final State BB = State.HEADER_SEGMENT_BEGIN;

    private static final State TS = State.TAG_SEARCH;
    private static final State T1 = State.TAG_1;
    private static final State T2 = State.TAG_2;
    private static final State T3 = State.TAG_3;
    private static final State SB = State.SEGMENT_BEGIN;
    private static final State DR = State.DATA_RELEASE;
    private static final State ED = State.ELEMENT_DATA;
    private static final State EI = State.ELEMENT_INVALID_DATA;
    private static final State CE = State.COMPONENT_END;
    private static final State ER = State.ELEMENT_REPEAT;
    private static final State EE = State.ELEMENT_END;
    private static final State SE = State.SEGMENT_END;
    private static final State SY = State.SEGMENT_EMPTY;

    private static final State TB = State.TRAILER_BEGIN;
    private static final State TD = State.TRAILER_ELEMENT_DATA;
    private static final State TE = State.TRAILER_ELEMENT_END;
    private static final State IE = State.INTERCHANGE_END;

    /**
     * BD - Binary Data
     */
    private static final State BD = State.ELEMENT_DATA_BINARY;

    /*
     * The state transition table takes the current state and the current
     * symbol, and returns either a new state or an action. An action is
     * represented as a negative number. An EDI text is accepted if at the end
     * of the text the state is initial and if the mode list is empty.
     */
    // @formatter:off
    /*-
     *                                                  SPACE                                               SEGMT   CMPST   RELSE   CNTRL   INVLD     *
     *                                                    |   A   B   D   E   I   N   S   T   U   X   Z       |       |       |       |       |       *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   | ALNUM | ELEMT | RPEAT | WHITE | OTHER | SEGTG *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   */
    /******************* Initial */
    private static final State[] FROM_INITIAL         = { II, __, __, __, __, X1, __, C1, __, U1, __, __, __, __, __, __, __, __, II, II, __, __, __ };
    /* ^ 0              */
    private static final State[] FROM_EDIFACT_1       = { __, __, __, __, __, __, U2, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_EDIFACT_2       = { __, IC, IC, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_TRADACOMS_1     = { __, __, __, __, __, __, __, __, C2, __, __, __, __, __, __, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_TRADACOMS_2     = { __, __, __, __, __, __, __, __, __, __, IC, __, __, __, __, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_X12_1           = { __, __, __, __, __, __, __, X2, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ };
    /* ^ 5              */
    private static final State[] FROM_X12_2           = { __, IC, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ };

    /******************* Common */
    private static final State[] FROM_HEADER          = { HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HZ, HE, HC, __, HR, HD, HD, HD, HV, HE };
    /* ^ 0              */
    private static final State[] FROM_HEADER_RELEASE  = { HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HV, HV, HD, HV, HD };
    private static final State[] FROM_TAG_1           = { __, T2, T2, T2, T2, T2, T2, T2, T2, T2, T2, T2, T2, _1, _1, __, __, __, __, __, __, __, _1 };
    private static final State[] FROM_TAG_2           = { __, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, SY, SB, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_TAG_3           = { __, __, __, __, __, __, __, __, __, __, __, __, __, SY, SB, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_ED              = { ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, SE, EE, CE, ER, DR, EI, EI, ED, EI, __ };
    /* ^ 5              */
    private static final State[] FROM_DR              = { ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, EI, EI, ED, EI, ED };
    private static final State[] FROM_BD              = { BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD };
    private static final State[] FROM_BE              = { __, __, __, __, __, __, __, __, __, __, __, __, __, SE, EE, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_TRAILER         = { TD, TD, TD, TD, TD, TD, TD, TD, TD, TD, TD, TD, TD, IE, TE, __, __, __, __, __, TD, __, __ };
    /* ^ 9              */

    /*-
     *                                                  SPACE                                               SEGMT   CMPST   RELSE   CNTRL   INVLD     *
     *                                                    |   A   B   D   E   I   N   S   T   U   X   Z       |       |       |       |       |       *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   | ALNUM | ELEMT | RPEAT | WHITE | OTHER | SEGTG *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   */
    /******************* EDIFACT */
    private static final State[] FROM_TS_EDIFACT      = { TS, T1, T1, T1, T1, T1, T1, T1, T1, U7, T1, T1, T1, __, __, __, __, __, TS, __, __, __, __ };
    /* ^ 10 (follows common) */
    private static final State[] FROM_EDIFACT_7       = { __, T2, T2, T2, T2, T2, U8, T2, T2, T2, T2, T2, T2, __, _1, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_EDIFACT_8       = { __, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, U9, T3, __, SB, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_EDIFACT_9       = { __, __, __, __, __, __, __, __, __, __, __, __, __, __, TB, __, __, __, __, __, __, __, __ };
//    private static final State[] FROM_EDIFACT_UNB_0   = { B0, __, __, __, __, __, __, __, B1, B1, __, __, __, __, __, __, __, __, B0, __, __, __, __ };
//    private static final State[] FROM_EDIFACT_UNB_1   = { __, __, __, __, __, __, B2, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ };
//    private static final State[] FROM_EDIFACT_UNB_2   = { __, __, B3, __, __, __, __, __, __, __, __, __, __, __, BB, __, __, __, __, __, __, __, __ };
//    private static final State[] FROM_EDIFACT_UNB_3   = { __, __, __, __, __, __, __, __, __, __, __, __, __, __, BB, __, __, __, __, __, __, __, __ };

    /*-
     *                                                  SPACE                                               SEGMT   CMPST   RELSE   CNTRL   INVLD     *
     *                                                    |   A   B   D   E   I   N   S   T   U   X   Z       |       |       |       |       |       *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   | ALNUM | ELEMT | RPEAT | WHITE | OTHER | SEGTG *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   */
    /******************* TRADACOMS */
    private static final State[] FROM_TS_TRADACOMS    = { TS, T1, T1, T1, C7, T1, T1, T1, T1, T1, T1, T1, T1, __, __, __, __, __, TS, __, __, __, __ };
    /* ^ 10 (follows common) */
    private static final State[] FROM_TRADACOMS_7     = { __, T2, T2, T2, T2, T2, C8, T2, T2, T2, T2, T2, T2, __, __, __, __, __, __, __, __, __, _1 };
    private static final State[] FROM_TRADACOMS_8     = { __, T3, T3, C9, T3, T3, T3, T3, T3, T3, T3, T3, T3, __, __, __, __, __, __, __, __, __, SB };
    private static final State[] FROM_TRADACOMS_9     = { __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, TB };
    private static final State[] FROM_TAG_2_TRADACOMS = { __, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, SY, __, __, __, __, __, __, __, __, SB };
    private static final State[] FROM_TAG_3_TRADACOMS = { __, __, __, __, __, __, __, __, __, __, __, __, __, SY, __, __, __, __, __, __, __, __, SB };

    /*-
     *                                                  SPACE                                               SEGMT   CMPST   RELSE   CNTRL   INVLD     *
     *                                                    |   A   B   D   E   I   N   S   T   U   X   Z       |       |       |       |       |       *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   | ALNUM | ELEMT | RPEAT | WHITE | OTHER | SEGTG *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   *
     *                                                    |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   */
    /******************* X12 */
    private static final State[] FROM_TS_X12          = { TS, T1, T1, T1, T1, X7, T1, T1, T1, T1, T1, T1, T1, __, __, __, __, __, TS, __, __, __, __ };
    /* ^ 10 (follows common) */
    private static final State[] FROM_X12_7           = { __, T2, T2, T2, X8, T2, T2, T2, T2, T2, T2, T2, T2, __, __, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_X12_8           = { __, X9, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, __, SB, __, __, __, __, __, __, __, __ };
    private static final State[] FROM_X12_9           = { __, __, __, __, __, __, __, __, __, __, __, __, __, __, TB, __, __, __, __, __, __, __, __ };

    private static final State[][] TRANSITION_INITIAL = {
        FROM_INITIAL,
        FROM_EDIFACT_1,
        FROM_EDIFACT_2,
        FROM_TRADACOMS_1,
        FROM_TRADACOMS_2,
        FROM_X12_1,
        FROM_X12_2
    };

    private static final State[][] TRANSITION_EDIFACT = {
        // Common
        FROM_HEADER,
        FROM_HEADER_RELEASE,
        FROM_TAG_1,
        FROM_TAG_2,
        FROM_TAG_3,
        FROM_ED,
        FROM_DR,
        FROM_BD,
        FROM_BE,
        FROM_TRAILER,
        // Dialect-specific
        FROM_TS_EDIFACT,
        FROM_EDIFACT_7,
        FROM_EDIFACT_8,
        FROM_EDIFACT_9,
//        FROM_EDIFACT_UNB_0,
//        FROM_EDIFACT_UNB_1,
//        FROM_EDIFACT_UNB_2,
//        FROM_EDIFACT_UNB_3
    };

    private static final State[][] TRANSITION_TRADACOMS = {
        // Common
        FROM_HEADER,
        FROM_HEADER_RELEASE,
        FROM_TAG_1,
        FROM_TAG_2_TRADACOMS, // Overrides common transitions
        FROM_TAG_3_TRADACOMS, // Overrides common transitions
        FROM_ED,
        FROM_DR,
        FROM_BD,
        FROM_BE,
        FROM_TRAILER,
        // Dialect-specific
        FROM_TS_TRADACOMS,
        FROM_TRADACOMS_7,
        FROM_TRADACOMS_8,
        FROM_TRADACOMS_9
    };

    private static final State[][] TRANSITION_X12 = {
        // Common
        FROM_HEADER,
        FROM_HEADER_RELEASE,
        FROM_TAG_1,
        FROM_TAG_2,
        FROM_TAG_3,
        FROM_ED,
        FROM_DR,
        FROM_BD,
        FROM_BE,
        FROM_TRAILER,
        // Dialect-specific
        FROM_TS_X12,
        FROM_X12_7,
        FROM_X12_8,
        FROM_X12_9
    };

    private static final State[][][] TRANSITIONS = {
        TRANSITION_INITIAL,
        TRANSITION_EDIFACT,
        TRANSITION_TRADACOMS,
        TRANSITION_X12
    };
    // @formatter:on

    private final int table;
    private final int code;

    State(int table, int code) {
        this.table = table;
        this.code = code;
    }

    State(int code) {
        this(-1, code);
    }

    public static State transition(State state, Dialect dialect, CharacterClass clazz) {
        if (state.table != -1) {
            /*
             * A state's table is set to force transition to another table. For example,
             * end of interchange states transition back to the unknown dialect transition
             * table.
             */
            return state.transition(state.table, clazz);
        }

        Objects.requireNonNull(dialect, "dialect was unexpectedly null");
        return state.transition(dialect.getDialectStateCode(), clazz);
    }

    public State transition(int dialect, CharacterClass clazz) {
        return TRANSITIONS[dialect][code][clazz.code];
    }

    public boolean isInvalid() {
        return Category.INVALID == code;
    }

    public boolean isHeaderState() {
        return Category.HEADER == code && DialectCode.UNKNOWN != table;
    }

}
