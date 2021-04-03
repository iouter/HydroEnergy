package com.sinthoras.hydroenergy.asm.biomesoplenty;

import com.sinthoras.hydroenergy.asm.HEPlugin;
import com.sinthoras.hydroenergy.asm.HEUtil;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

import java.util.List;

public class FogHandlerTransformer {

    /* Replace
     * blockAtEyes.getMaterial() == Material.water
     * with
     * HEGetMaterialUtil.getMaterialWrapper(event) == Material.water
     */
    public static byte[] transform(byte[] basicClass, boolean isObfuscated) {
        final String CLASS_FogColors = "net/minecraftforge/client/event/EntityViewRenderEvent$FogColors";
        final String CLASS_Material = "net/minecraft/block/material/Material";
        final String CLASS_HEGetMaterialUtil = "com/sinthoras/hydroenergy/api/HEGetMaterialUtil";
        final String CLASS_Block = "net/minecraft/block/Block";

        final ClassNode classNode = HEUtil.convertByteArrayToClassNode(basicClass);

        final String METHOD_getMaterialHEWrapper = "getMaterialHEWrapper";
        final String METHOD_getMaterialHEWrapper_DESC = "(L" + CLASS_FogColors + ";)L" + CLASS_Material + ";";
        final boolean isApiUsed = null != HEUtil.getMethod(classNode, METHOD_getMaterialHEWrapper, METHOD_getMaterialHEWrapper_DESC);
        if(isApiUsed) {
            HEPlugin.info("Biomes 'o Plenty is using HydroEnergy API. No injection neccessary.");
            return basicClass;
        }

        final String MARKER_method = "onGetFogColour";
        final String MARKER_method_DESC = "(L" + CLASS_FogColors + ";)V";
        final MethodNode targetMethod = HEUtil.getMethod(classNode, MARKER_method, MARKER_method_DESC);
        if(targetMethod == null) {
            HEPlugin.info("Could not find injection target method in Biomes 'o Plenty. You will experience visual bugs.");
            return basicClass;
        }

        final boolean isStatic = false;
        final String MARKER_instruction_OWNER = CLASS_Block;
        final String MARKER_instruction = isObfuscated ? "func_149688_o" : "getMaterial";
        final String MARKER_instruction_DESC = "()L" + CLASS_Material + ";";
        List<MethodInsnNode> instructions = HEUtil.getInstructions(targetMethod, isStatic, MARKER_instruction_OWNER, MARKER_instruction, MARKER_instruction_DESC);
        if(instructions.size() != 2) {
            HEPlugin.info("Could not find injection target instruction in Biomes 'o Plenty. You will experience visual bugs.");
            return basicClass;
        }

        final String REPLACED_method = "getMaterialWrapper";
        final String REPLACED_method_DESC = "(L" + CLASS_FogColors + ";)L" + CLASS_Material + ";";
        InsnList instructionToInsert = new InsnList();
        instructionToInsert.add(new VarInsnNode(ALOAD, 1));
        instructionToInsert.add(new MethodInsnNode(INVOKESTATIC,
                CLASS_HEGetMaterialUtil,
                REPLACED_method,
                REPLACED_method_DESC,
                false));
        // Add instruction after target instruction
        targetMethod.instructions.insert(instructions.get(1), instructionToInsert);
        // Remove ALOAD before target instruction
        targetMethod.instructions.remove(instructions.get(1).getPrevious());
        // Remove target instruction itself
        targetMethod.instructions.remove(instructions.get(1));
        HEPlugin.info("Fixed mod-interop with Biomes 'o Plenty.");

        return HEUtil.convertClassNodeToByteArray(classNode);
    }
}
