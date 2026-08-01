package com.omsent.addon;

import com.omsent.addon.modules.*;
import com.omsent.addon.GliemtUtils.DebugUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class AddonTemplate extends MeteorAddon {
    public static final Category CATEGORY = new Category("oms addon");
    @Override
    public void onInitialize() {

        // Modules
        Modules.get().add(new Main());
        Modules.get().add(new NoGround());
        Modules.get().add(new AntiLag());
        Modules.get().add(new TPBot());
        Modules.get().add(new NoLoadScreen());
        //Modules.get().add(new Info());
        Modules.get().add(new Predict());
        Modules.get().add(new AutoUseTimer());
        Modules.get().add(new Follower());
        Modules.get().add(new VclipPlus());
        Modules.get().add(new AutoWarm());
        Modules.get().add(new PhaseCheck());
        Modules.get().add(new AntiCrash());
        Modules.get().add(new AutoPot());
        Modules.get().add(new TurtlePotStatic());
        Modules.get().add(new CrystalBooster());
        Modules.get().add(new AnchorHelper());
        Modules.get().add(new SetVel_qwq());
        Modules.get().add(new ChatEncrypt());
        //Modules.get().add(new FaceBlocker());
        //Modules.get().add(new AutoSwingGate());
        if(DebugUtils.isDebugMode()) {
            String outputText;
            int outpoutCount;
            if (DebugUtils.isDebugMode()) {
                outpoutCount = 10;
                outputText = "Debug mode enabled!";
            } else {
                outpoutCount = 1;
                outputText = "Now is release version!";
            }

            //Debug Modules
            Modules.get().add(new DebugMode());
            Modules.get().add(new Debug_Placement());
            for (int i = 1;i <= outpoutCount;i++) {
                System.out.println(outputText);
            }
        }

        // Commands

        // HUD
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.omsent.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("omsent", "omsent_addon");
    }


}
