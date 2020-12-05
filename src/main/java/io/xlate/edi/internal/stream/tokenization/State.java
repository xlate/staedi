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
    INTERCHANGE_END(0),

    HEADER_X12_I(1),
    HEADER_X12_S(2),
    TRAILER_X12_I(3),
    TRAILER_X12_E(4),
    TRAILER_X12_A(5),

    HEADER_EDIFACT_U(6),
    HEADER_EDIFACT_N(7),
    TRAILER_EDIFACT_U(8),
    TRAILER_EDIFACT_N(9),
    TRAILER_EDIFACT_Z(10),

    HEADER_TRADACOMS_S(11),
    HEADER_TRADACOMS_T(12),
    TRAILER_TRADACOMS_E(13),
    TRAILER_TRADACOMS_N(14),
    TRAILER_TRADACOMS_D(15),

    // start at last header_tag + 1
    INTERCHANGE_CANDIDATE(16), // IC
    HEADER_DATA(16), // HD
    HEADER_SEGMENT_BEGIN(16),
    HEADER_INVALID_DATA(16), // HV
    HEADER_COMPONENT_END(16), // HC
    HEADER_ELEMENT_END(16), // HE
    HEADER_SEGMENT_END(16),

    HEADER_TAG_SEARCH(17),
    HEADER_TAG_1(18),
    HEADER_TAG_2(19),
    HEADER_TAG_3(20),

    TAG_SEARCH(21),
    SEGMENT_END(21),
    SEGMENT_EMPTY(21),
    TAG_1(22),
    // TODO: Clear ELEMT for TAG_2 and TAG_3 states for TRADACOMS (requires dialect-specific transition tables)
    TAG_2(23),
    TAG_3(24),

    SEGMENT_BEGIN(25),
    ELEMENT_DATA(25),
    ELEMENT_INVALID_DATA(25),
    COMPONENT_END(25),
    ELEMENT_REPEAT(25),
    ELEMENT_END(25),

    // TODO: Data Release (DR) needs a header equivalent
    DATA_RELEASE(26),

    ELEMENT_DATA_BINARY(27),
    ELEMENT_END_BINARY(28),

    TRAILER_BEGIN(29),
    TRAILER_ELEMENT_DATA(29),
    TRAILER_ELEMENT_END(29);

    private static final State __ = State.INVALID;

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
    private static final State[][] TRANSITION_TABLE = {
        /*-
         *                     SPACE                                               SEGMT   CMPST   RELSE   CNTRL   INVLD     *
         *                       |   A   B   D   E   I   N   S   T   U   X   Z       |       |       |       |       |       *
         *                       |   |   |   |   |   |   |   |   |   |   |   | ALNUM | ELEMT | RPEAT | WHITE | OTHER | SEGTG *
         *                       |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   *
         *                       |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   */
        /* II | IE Initial  */ { II, __, __, __, __, X1, __, C1, __, U1, __, __, __, __, __, __, __, __, II, II, __, __, __ },

        /* X1 (ISA / I)     */ { __, __, __, __, __, __, __, X2, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* X2 (ISA / S)     */ { __, IC, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* X7 (IEA / I)     */ { __, T2, T2, T2, X8, T2, T2, T2, T2, T2, T2, T2, T2, __, __, __, __, __, __, __, __, __, __ },
        /* X8 (IEA / E)     */ { __, X9, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, __, SB, __, __, __, __, __, __, __, __ },
        /* X9 (IEA / A)     */ { __, __, __, __, __, __, __, __, __, __, __, __, __, __, TB, __, __, __, __, __, __, __, __ },
        /* ^ 5              */
        /* U1 (UNB / U)     */ { __, __, __, __, __, __, U2, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* U2 (UNB / N)     */ { __, IC, IC, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* U7 (UNZ / U)     */ { __, T2, T2, T2, T2, T2, U8, T2, T2, T2, T2, T2, T2, __, __, __, __, __, __, __, __, __, __ },
        /* U8 (UNZ / N)     */ { __, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, U9, T3, __, SB, __, __, __, __, __, __, __, __ },
        /* U9 (UNZ / Z)     */ { __, __, __, __, __, __, __, __, __, __, __, __, __, __, TB, __, __, __, __, __, __, __, __ },
        /* ^ 10             */
        /* C1 (STX / S)     */ { __, __, __, __, __, __, __, __, C2, __, __, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* C2 (STX / T)     */ { __, __, __, __, __, __, __, __, __, __, IC, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* C7 (END / E)     */ { __, T2, T2, T2, T2, T2, C8, T2, T2, T2, T2, T2, T2, __, __, __, __, __, __, __, __, __, __ },
        /* C8 (END / N)     */ { __, T3, T3, C9, T3, T3, T3, T3, T3, T3, T3, T3, T3, __, __, __, __, __, __, __, __, __, SB },
        /* C9 (END / D)     */ { __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, TB },
        /* ^ 15             */
        /* IC | HD          */ { HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HD, HZ, HE, HC, __, DR, HD, HD, HD, HV, HE },
        /* B0 (Header Search*/ { B0, __, __, __, __, __, __, __, B1, B1, __, __, __, __, __, __, __, __, B0, __, __, __, __ },
        /* B1 (UNB / U)     */ { __, __, __, __, __, __, B2, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __ },
        /* B2 (UNB / N)     */ { __, __, B3, __, __, __, __, __, __, __, __, __, __, __, BB, __, __, __, __, __, __, __, __ },
        /* B3 (UNB / B)     */ { __, __, __, __, __, __, __, __, __, __, __, __, __, __, BB, __, __, __, __, __, __, __, __ },
        /* ^ 20             */
        /* SE+TS Tag Search */ { TS, T1, T1, C7, C7, X7, T1, T1, T1, U7, T1, T1, T1, __, __, __, __, __, TS, __, __, __, __ },
        /* T1  Tag Char 1 * */ { __, T2, T2, T2, T2, T2, T2, T2, T2, T2, T2, T2, T2, __, __, __, __, __, __, __, __, __, __ },
        /* T2  Tag Char 2 * */ { __, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, T3, SY, SB, __, __, __, __, __, __, __, SB },
        /* T3  Tag Char 3 * */ { __, __, __, __, __, __, __, __, __, __, __, __, __, SY, SB, __, __, __, __, __, __, __, SB },
        /* Element Process  */ { ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, SE, EE, CE, ER, DR, EI, EI, ED, EI, __ },
        /* ^ 25             */
        /* Data Release     */ { ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, ED, EI, EI, ED, EI, ED },
        /* Binary Data      */ { BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD, BD },
        /* Binary Data End  */ { __, __, __, __, __, __, __, __, __, __, __, __, __, SE, EE, __, __, __, __, __, __, __, __ },
        /* TB | TD | TE IEA */ { TD, TD, TD, TD, TD, TD, TD, TD, TD, TD, TD, TD, TD, IE, TE, __, __, __, __, __, TD, __, __ }
        /* ^ 29             */
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
