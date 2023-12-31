/*
 Some of the tests in this file are adapted from Apache's Commons Lang3 tests, which has the following license:

                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.
*/
package dinkplugin.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void truncate() {
        assertEquals("Hello world", Utils.truncate("Hello world", 200));
        assertEquals("Hello world", Utils.truncate("Hello world", 11));
        assertEquals("Helloworld", Utils.truncate("Helloworld", 10));
        assertEquals("Hellowor…", Utils.truncate("Helloworld", 9));
        assertEquals("Hello…", Utils.truncate("Hello world", 9));
        assertEquals("Hello…", Utils.truncate("Hello worldly beings", 10));
        assertEquals("Hello worldly…", Utils.truncate("Hello worldly beings", 16));
        assertEquals("Hello worldly…", Utils.truncate("Hello worldly beings", 15));
        assertEquals("Hello worldly…", Utils.truncate("Hello worldly beings", 14));
        assertEquals("Hello…", Utils.truncate("Hello worldly beings", 13));
    }

    @Test
    void regexify() {
        Pattern a = Utils.regexify("Hello world!");
        assertEquals("^\\QHello world!\\E$", a.pattern());
        assertTrue(a.matcher("Hello world!").find());
        assertTrue(a.matcher("hello world!").find());
        assertFalse(a.matcher("Hello world").find());
        assertFalse(a.matcher("Hello world!!").find());
        assertFalse(a.matcher("Hi Hello world!").find());

        Pattern b = Utils.regexify("Hello.world!");
        assertEquals("^\\QHello.world!\\E$", b.pattern());
        assertTrue(b.matcher("Hello.world!").find());
        assertFalse(b.matcher("Hello world!").find());
        assertFalse(b.matcher("Hello.world!!").find());

        Pattern c = Utils.regexify("Hello world!*");
        assertEquals("^\\QHello world!\\E.*", c.pattern());
        assertTrue(c.matcher("Hello world!").find());
        assertTrue(c.matcher("Hello world!~").find());
        assertFalse(c.matcher("Hi Hello world!").find());
        assertFalse(c.matcher("Hello world").find());

        Pattern d = Utils.regexify("*Hello world!");
        assertEquals("\\QHello world!\\E$", d.pattern());
        assertTrue(d.matcher("Hello world!").find());
        assertTrue(d.matcher("Hi Hello world!").find());
        assertFalse(d.matcher("Hello world!!").find());
        assertFalse(d.matcher("Hello world").find());

        Pattern e = Utils.regexify("*Hello world!*");
        assertEquals("\\QHello world!\\E.*", e.pattern());
        assertTrue(e.matcher("Hello world!").find());
        assertTrue(e.matcher("Hi Hello world!").find());
        assertTrue(e.matcher("Hello world!!").find());
        assertTrue(e.matcher("Hi Hello world!!").find());
        assertFalse(e.matcher("Hi Hello cruel world!!").find());

        Pattern f = Utils.regexify("*Hello*world!*");
        assertEquals("\\QHello\\E.*\\Qworld!\\E.*", f.pattern());
        assertTrue(f.matcher("Hello world!").find());
        assertTrue(f.matcher("Hi Hello world!").find());
        assertTrue(f.matcher("Hello world!!").find());
        assertTrue(f.matcher("Hi Hello world!!").find());
        assertTrue(f.matcher("Hi hello World!!").find());
        assertTrue(f.matcher("Hi Hello cruel world!!").find());

        Pattern g = Utils.regexify("Membership's price is $12.49");
        assertTrue(g.matcher("Membership's price is $12.49").find());
        assertFalse(g.matcher("Membership's price is $12.499").find());
        assertFalse(g.matcher("Membershipss price is $12.49").find());
        assertFalse(g.matcher("Membership's price is $12349").find());

        Pattern h = Utils.regexify("Membership's price is $12.49*");
        assertTrue(h.matcher("Membership's price is $12.49").find());
        assertTrue(h.matcher("Membership's price is $12.499").find());
        assertFalse(h.matcher("A Membership's price is $12.49").find());

        Pattern i = Utils.regexify("dragon*");
        assertTrue(i.matcher("dragon pickaxe").find());
        assertTrue(i.matcher("dragon claws").find());
        assertFalse(i.matcher("iron pickaxe").find());

        Pattern j = Utils.regexify("*orb");
        assertTrue(j.matcher("awakener's orb").find());
        assertTrue(j.matcher("commorb").find());
        assertFalse(j.matcher("commorb v2").find());

        Pattern k = Utils.regexify("vorkath's head");
        assertTrue(k.matcher("vorkath's head").find());
        assertFalse(k.matcher("vorki").find());
        assertFalse(k.matcher("iron pickaxe").find());

        Pattern l = Utils.regexify("clue scroll*");
        assertTrue(l.matcher("clue scroll (elite)").find());
        assertTrue(l.matcher("clue scroll (beginner)").find());
        assertTrue(l.matcher("clue scroll (easy)").find());
        assertFalse(l.matcher("clue bottle (beginner)").find());
        assertFalse(l.matcher("iron pickaxe").find());

        Pattern m = Utils.regexify("jar of*");
        assertTrue(m.matcher("jar of dirt").find());
        assertTrue(m.matcher("jar of smoke").find());
        assertTrue(m.matcher("jar of dust").find());
        assertTrue(m.matcher("jar of spirits").find());
        assertFalse(m.matcher("iron pickaxe").find());
    }

    @Test
    void sanitize() {
        assertEquals("Congratulations, you've unlocked a new Relic: Archer's Embrace.", Utils.sanitize("Congratulations, you've unlocked a new Relic: <col=ff7700>Archer's Embrace</col>."));
        assertEquals("Congratulations, you've completed an easy task: Obtain a Gem While Mining.", Utils.sanitize("Congratulations, you've completed an easy task: <col=ff7700>Obtain a Gem While Mining</col>."));

        assertEquals("", Utils.sanitize(null));
        assertEquals("", Utils.sanitize(""));

        assertEquals("foo\nbar", Utils.sanitize("foo<br>bar"));

        assertEquals("foo bar", Utils.sanitize("foo\u00A0bar"));
    }

    /**
     * @see <a href="https://github.com/apache/commons-lang/blob/master/src/test/java/org/apache/commons/lang3/StringUtilsTest.java">StringUtilsTest</a>
     */
    @Test
    void padRight() {
        assertEquals("     ", Utils.padRight("", 5, ' '));
        assertEquals("abc  ", Utils.padRight("abc", 5, ' '));
        assertEquals("abc", Utils.padRight("abc", 2, ' '));
        assertEquals("abc", Utils.padRight("abc", -1, ' '));
        assertEquals("abcxx", Utils.padRight("abc", 5, 'x'));
        final String str = Utils.padRight("aaa", 10000, 'a');  // bigger than pad length
        assertEquals(10000, str.length());
        assertTrue(str.chars().allMatch(i -> i == 'a'));
    }

    /**
     * @see <a href="https://github.com/apache/commons-lang/blob/master/src/test/java/org/apache/commons/lang3/StringUtilsTest.java">StringUtilsTest</a>
     */
    @Test
    void normalizeSpace() {
        final char nbsp = (char) 160;
        assertFalse(Character.isWhitespace(nbsp));
        assertNull(Utils.normalizeSpace(null));
        assertEquals("", Utils.normalizeSpace(""));
        assertEquals("", Utils.normalizeSpace(" "));
        assertEquals("", Utils.normalizeSpace("\t"));
        assertEquals("", Utils.normalizeSpace("\n"));
        assertEquals("", Utils.normalizeSpace("\u000B"));
        assertEquals("", Utils.normalizeSpace("\u000C"));
        assertEquals("", Utils.normalizeSpace("\u001C"));
        assertEquals("", Utils.normalizeSpace("\u001D"));
        assertEquals("", Utils.normalizeSpace("\u001E"));
        assertEquals("", Utils.normalizeSpace("\u001F"));
        assertEquals("", Utils.normalizeSpace("\f"));
        assertEquals("", Utils.normalizeSpace("\r"));
        assertEquals("a", Utils.normalizeSpace("  a  "));
        assertEquals("a b c", Utils.normalizeSpace("  a  b   c  "));
        assertEquals("a b c", Utils.normalizeSpace("a\t\f\r  b\u000B   c\n"));
        assertEquals("a   b c", Utils.normalizeSpace("a\t\f\r  " + nbsp + nbsp + "b\u000B   c\n"));
        assertEquals("b", Utils.normalizeSpace("\u0000b"));
        assertEquals("b", Utils.normalizeSpace("b\u0000"));
    }

}
