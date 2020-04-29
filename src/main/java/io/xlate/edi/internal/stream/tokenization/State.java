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

/*
 * Processing states.
 */
public enum State {
    INVALID(-1),

    INITIAL(0),

    HEADER_TAG_I(1),
    HEADER_TAG_N(2),
    HEADER_TAG_S(3),
    HEADER_TAG_U(4),

    // start at last header_tag + 1
    INTERCHANGE_CANDIDATE(5), // IC
    HEADER_DATA(5), // HD
    HEADER_SEGMENT_BEGIN(5),
    HEADER_INVALID_DATA(5), // HV
    HEADER_COMPONENT_END(5), // HC
    HEADER_ELEMENT_END(5), // HE
    HEADER_SEGMENT_END(5),

    HEADER_TAG_SEARCH(6),
    HEADER_TAG_1(7),
    HEADER_TAG_2(8),
    HEADER_TAG_3(9),

    TAG_SEARCH(10),
    TAG_1(11),
    TAG_2(12),
    TAG_3(13),

    SEGMENT_BEGIN(14),

    ELEMENT_DATA(14),
    ELEMENT_INVALID_DATA(14),
    COMPONENT_END(14),
    ELEMENT_REPEAT(14),
    ELEMENT_END(14),

    DATA_RELEASE(15),

    ELEMENT_DATA_BINARY(16),
    ELEMENT_END_BINARY(17),

    SEGMENT_END(10),
    SEGMENT_EMPTY(10),

    TRAILER_TAG_I(18),
    TRAILER_TAG_E(19),
    TRAILER_TAG_A(20),

    TRAILER_TAG_U(21),
    TRAILER_TAG_N(22),
    TRAILER_TAG_Z(23),

    TRAILER_BEGIN(24),
    TRAILER_ELEMENT_DATA(24),
    TRAILER_ELEMENT_END(24),

    INTERCHANGE_END(0);

    private static final State __ = State.INVALID;

    private static final State II = State.INITIAL;

    private static final State HI = State.HEADER_TAG_I;
    private static final State HN = State.HEADER_TAG_N;
    private static final State HS = State.HEADER_TAG_S;
    private static final State HU = State.HEADER_TAG_U;

    private static final State IC = State.INTERCHANGE_CANDIDATE;

    private static final State HD = State.HEADER_DATA;
    private static final State HV = State.HEADER_INVALID_DATA;
    private static final State HC = State.HEADER_COMPONENT_END;
    private static final State HE = State.HEADER_ELEMENT_END;
    private static final State HZ = State.HEADER_SEGMENT_END;

    private static final State B0 = State.HEADER_TAG_SEARCH;
    private static final State B1 = State.HEADER_TAG_1;
    private static final State B2 = State.HEADER_TAG_2;
    private static final State B3 = State.HEADER_TAG_3;
    private static final State BB = State.HEADER_SEGMENT_BEGIN;

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

    private static final State ZI = State.TRAILER_TAG_I;
    private static final State ZE = State.TRAILER_TAG_E;
    private static final State ZA = State.TRAILER_TAG_A;

    private static final State ZU = State.TRAILER_TAG_U;
    private static final State ZN = State.TRAILER_TAG_N;
    private static final State ZZ = State.TRAILER_TAG_Z;

    private static final State TB = State.TRAILER_BEGIN;
    private static final State TD = State.TRAILER_ELEMENT_DATA;
    private static final State TE = State.TRAILER_ELEMENT_END;
    private static final State IE = State.INTERCHANGE_END;

    private static final State BD = State.ELEMENT_DATA_BINARY;

    /*
     * The state transition table takes the current state and the current
     * symbol, and returns either a new state or an action. An action is
     * represented as a negative number. An EDI text is accepted if at the end
     * of the text the state is initial and if the mode list is empty.
     */
    // @formatter:off
    private static final State[][] TRANSITION_TABLE = {
        /*-
         *                     SPACE ~   B   ~   I   ~   S   ~   Z   ~ SEGMT ~ CMPST ~ RELSE ~ CNTRL ~ INVLD *
         *                       |       |       |       |       |       |       |       |       |       |   *
         *                       |   A   |   E   |   N   |   U   | ALNUM | ELEMT | REP_E | WHITE | OTHER |   *
         *                       |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   *
         *                       | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ |   |   |   |   |   |   |   |   */
        /* II | IE Initial  */{ II, __, __, __, HI, __, __, HU, __, __, __, __, __, __, __, II, II, __, __ },

        /* ISA / I       -> */{ __, __, __, __, __, __, HS, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* UNB / N       -> */{ __, IC, IC, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* ISA / S       -> */{ __, IC, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* UNB / U       -> */{ __, __, __, __, __, HN, __, __, __, __, __, __, __, __, __, __, __, __, __ },

        /* IC | HD          */{ HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HZ, HE, HC, __, __, HD, HD, HD, HV },

        /* B0  Header Search*/{ B0, __, __, __, __, __, __, B1, __, __, __, __, __, __, __, B0, __, __, __ },
        /* B1  UNB (U)      */{ __, __, __, __, __, B2, __, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* B2  UNB (N)      */{ __, __, B3, __, __, __, __, __, __, __, __, BB, __, __, __, __, __, __, __ },
        /* B3  UNB (B)      */{ __, __, __, __, __, __, __, __, __, __, __, BB, __, __, __, __, __, __, __ },

        /* SE+TS Tag Search */{ TS, T1, T1, T1, ZI, T1, T1, ZU, T1, T1, __, __, __, __, __, TS, __, __, __ },
        /* T1  Tag Char 1 * */{ __, T2, T2, T2, T2, T2, T2, T2, T2, T2, __, __, __, __, __, __, __, __, __ },
        /* T2  Tag Char 2 * */{ __, T3, T3, T3, T3, T3, T3, T3, T3, T3, SY, SB, __, __, __, __, __, __, __ },
        /* T3  Tag Char 3 * */{ __, __, __, __, __, __, __, __, __, __, SY, SB, __, __, __, __, __, __, __ },

        /* Element Process  */{ ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, SE, EE, CE, ER, DR, EI, EI, ED, EI },
        /* Data Release     */{ ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, EI, EI, ED, EI },

        /* Binary Data      */{ BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD },
        /* Binary Data End  */{ __, __, __, __, __, __, __, __, __, __, SE, EE, __, __, __, __, __, __, __ },

        /* IEA / I          */{ __, T2, T2, ZE, T2, T2, T2, T2, T2, T2, __, __, __, __, __, __, __, __, __ },
        /* IEA / E          */{ __, ZA, T3, T3, T3, T3, T3, T3, T3, T3, __, SB, __, __, __, __, __, __, __ },
        /* IEA / A          */{ __, __, __, __, __, __, __, __, __, __, __, TB, __, __, __, __, __, __, __ },
        /* UNZ / U          */{ __, T2, T2, T2, T2, ZN, T2, T2, T2, T2, __, __, __, __, __, __, __, __, __ },
        /* UNZ / N          */{ __, T3, T3, T3, T3, T3, T3, T3, ZZ, T3, __, SB, __, __, __, __, __, __, __ },
        /* UNZ / Z          */{ __, __, __, __, __, __, __, __, __, __, __, TB, __, __, __, __, __, __, __ },
        /* TB | TD | TE IEA */{ TD, TD, TD, TD, TD, TD, TD, TD, TD, TD, IE, TE, __, __, __, __, __, TD, __ }
        };
    // @formatter:on

    private int code;

    State(int code) {
        this.code = code;
    }

    public State transition(CharacterClass clazz) {
        return TRANSITION_TABLE[code][clazz.code];
    }
}
