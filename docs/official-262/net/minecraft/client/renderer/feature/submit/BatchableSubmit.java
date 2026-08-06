/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.feature.submit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;

@Environment(value=EnvType.CLIENT)
public interface BatchableSubmit
extends SubmitNode {
    public Object batchKey();

    public FeatureRendererType<? extends BatchableSubmit> featureType();
}

