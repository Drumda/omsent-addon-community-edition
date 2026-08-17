package com.omsent.addon.modules;

import com.omsent.addon.NModule;

public class TpBotV2 extends NModule{
  public static getInstance() {return INSTANCE;}
  public TpBotV2() super("TpBotV2", "new update!");

  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  // 韩家慧你先merge我的更改 这个功能你不要动 我后面会给他写完的
  
  private final Setting<Integer> speed = sgGeneral.add(new BoolSetting.Builder(
    .name("Speed")
    .descrption("?")
    .defaultValue(1)
    .min(0.1)
    .max(60.0)
    .sliderRange(0.1, 60.0)
    .build()                                               
  ));
}
