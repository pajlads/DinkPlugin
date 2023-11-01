/*
 * Adapted from https://github.com/runelite/runelite/pull/11580
 * which is available under the following license:
 *
 * BSD 2-Clause License
 *
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Lotto <https://github.com/devLotto>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dinkplugin.util;

import com.google.common.collect.ImmutableList;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class QuestUtils {

    private final Pattern QUEST_PATTERN_1 = Pattern.compile(".+?ve\\.*? (?<verb>been|rebuilt|.+?ed)? ?(?:the )?'?(?<quest>.+?)'?(?: [Qq]uest)?[!.]?$");
    private final Pattern QUEST_PATTERN_2 = Pattern.compile("'?(?<quest>.+?)'?(?: [Qq]uest)? (?<verb>[a-z]\\w+?ed)?(?: f.*?)?[!.]?$");
    private final Collection<String> RFD_TAGS = ImmutableList.of("Another Cook", "freed", "defeated", "saved");
    private final Collection<String> WORD_QUEST_IN_NAME_TAGS = ImmutableList.of("Another Cook", "Doric", "Heroes", "Legends", "Observatory", "Olaf", "Waterfall");
    private final Map<String, String> QUEST_REPLACEMENTS = Map.of(
        "Lumbridge Cook... again", "Another Cook's",
        "Skrach 'Bone Crusher' Uglogwee", "Skrach Uglogwee"
    );

    @Nullable
    public String parseQuestWidget(final String text) {
        Matcher matcher = getMatcher(text);
        if (matcher == null) {
            log.warn("Unable to match quest: {}", text);
            return null;
        }

        String quest = matcher.group("quest");
        quest = QUEST_REPLACEMENTS.getOrDefault(quest, quest);

        String verb = StringUtils.defaultString(matcher.group("verb"));

        if (verb.contains("kind of")) {
            log.debug("Skipping partial completion of quest: {}", quest);
            return null;
        } else if (verb.contains("completely")) {
            quest += " II";
        }

        if (RFD_TAGS.stream().anyMatch((quest + verb)::contains)) {
            quest = "Recipe for Disaster - " + quest;
        }

        if (WORD_QUEST_IN_NAME_TAGS.stream().anyMatch(quest::contains)) {
            quest += " Quest";
        }

        return quest;
    }

    @Nullable
    private Matcher getMatcher(String text) {
        if (text == null)
            return null;

        // "You have completed The Corsair Curse!"
        Matcher questMatch1 = QUEST_PATTERN_1.matcher(text);
        if (questMatch1.matches())
            return questMatch1;

        // "'One Small Favour' completed!"
        Matcher questMatch2 = QUEST_PATTERN_2.matcher(text);
        if (questMatch2.matches())
            return questMatch2;

        return null;
    }

}
