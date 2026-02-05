package com.xyoye.danmaku.filter;

import com.xyoye.common_component.log.LogFacade;
import com.xyoye.common_component.log.model.LogModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import master.flame.danmaku.controller.DanmakuFilters;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;

/**
 * Created by xyoye on 2021/2/25.
 */

public class RegexFilter extends DanmakuFilters.BaseDanmakuFilter<List<String>> {
    private static final int FILTER_TYPE_REGEX = 2048;
    public final List<String> mRegexList = new ArrayList<>();

    @Override
    public boolean filter(BaseDanmaku danmaku, int index, int totalsizeInScreen, DanmakuTimer timer, boolean fromCachingTask, DanmakuContext config) {
        boolean filtered = false;
        for (int i = 0; i < mRegexList.size(); i++) {
            String regex = mRegexList.get(i);

            try {
                if (danmaku.text == null) {
                    continue;
                }
                filtered = Pattern.matches(regex, danmaku.text);
            } catch (PatternSyntaxException e) {
                LogFacade.INSTANCE.w(
                        LogModule.PLAYER,
                        "RegexFilter",
                        "Invalid regex pattern, removed",
                        Collections.singletonMap("regex", regex),
                        e
                );
                mRegexList.remove(i);
                i--;
            } catch (Exception e) {
                LogFacade.INSTANCE.w(
                        LogModule.PLAYER,
                        "RegexFilter",
                        "Regex match failed",
                        Collections.emptyMap(),
                        e
                );
            }
            if (filtered) {
                logDebug(danmaku.text.toString());
                break;
            }
        }
        if (filtered) {
            danmaku.mFilterParam |= FILTER_TYPE_REGEX;
        }
        return filtered;
    }

    @Override
    public void setData(List<String> data) {
        reset();
        if (data != null) {
            for (String i : data) {
                addRegex(i);
            }
        }
    }

    @Override
    public void reset() {
        mRegexList.clear();
    }

    public void addRegex(String regex) {
        if (!mRegexList.contains(regex)) {
            mRegexList.add(regex);
        }
    }

    public void removeRegex(String regex) {
        mRegexList.remove(regex);
    }

    private void logDebug(String message) {
        LogFacade.INSTANCE.d(
                LogModule.PLAYER,
                "RegexFilter",
                message,
                Collections.emptyMap(),
                null
        );
    }
}
