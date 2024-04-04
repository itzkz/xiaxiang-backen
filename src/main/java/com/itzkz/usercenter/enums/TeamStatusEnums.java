package com.itzkz.usercenter.enums;

/**
 * 队伍状态枚举
 */
public enum TeamStatusEnums {
    PUBLIC(0, "公开"),
    PRIVATE(1, "私密"),
    SECRET(2, "加密");
    /**
     * 状态码
     */
    private int value;

    /**
     * 信息文本
     */

    private String text;

    TeamStatusEnums(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static TeamStatusEnums getTeamStatusEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }

        TeamStatusEnums[] values = TeamStatusEnums.values();
        for (TeamStatusEnums teamStatusEnums : values) {
            if (teamStatusEnums.getValue() == value) {
                return teamStatusEnums;
            }
        }
        return null;

    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
