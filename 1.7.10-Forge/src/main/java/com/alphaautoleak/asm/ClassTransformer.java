package com.alphaautoleak.asm;

import com.alphaautoleak.events.EventReceiveMessage;
import com.alphaautoleak.events.EventSendMessage;
import com.darkmagician6.eventapi.EventManager;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.EnumMap;
import java.util.function.BiConsumer;

/**
 * @Author: SnowFlake
 * @Date: 2022/6/3 18:02
 */
public class ClassTransformer implements IClassTransformer, Opcodes
{

    @Override
    public byte[] transform(String name, String transformedName, byte[] classByte)
    {
        // 1.7.10
        if (name.equals("cpw.mods.fml.common.network.simpleimpl.SimpleChannelHandlerWrapper"))
        {
            return transformMethods(classByte,this::transformSimpleChannelHandlerWrapper1_7_10);
        }
        if (name.equals("cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper"))
        {
            return transformMethods(classByte,this::transformSimpleNetworkWrapper1_7_10);
        }



        return classByte;
    }

    public void transformSimpleChannelHandlerWrapper1_7_10(ClassNode classNode, MethodNode methodNode) {
        if (methodNode.name.equalsIgnoreCase("channelRead0")) {

            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(ALOAD,2));

            insnList.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(ClassTransformer.class), "simpleChannelHandlerWrapper", "(Ljava/lang/Object;)Z", false));
            LabelNode labelNode = new LabelNode();
            insnList.add(new JumpInsnNode(IFEQ,labelNode));
            insnList.add(new InsnNode(RETURN));
            insnList.add(labelNode);
            insnList.add(new FrameNode(F_SAME, 0, null, 0, null));
            methodNode.instructions.insert(insnList);
        }
    }


    public static boolean simpleChannelHandlerWrapper(Object object){
        EventReceiveMessage eventReceiveMessage = new EventReceiveMessage(object);
        EventManager.call(eventReceiveMessage);
        return eventReceiveMessage.isCancelled();
    }

    public void transformSimpleNetworkWrapper1_7_10(ClassNode classNode, MethodNode methodNode) {
        if (methodNode.name.equalsIgnoreCase("sendToServer")) {

            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(ALOAD,1));

            insnList.add(new VarInsnNode(ALOAD,0));
            insnList.add(new FieldInsnNode(GETFIELD, "cpw/mods/fml/common/network/simpleimpl/SimpleNetworkWrapper", "channels", "Ljava/util/EnumMap;"));

            insnList.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(ClassTransformer.class), "simpleNetworkWrapperHook", "(Ljava/lang/Object;Ljava/util/EnumMap;)Z", false));
            LabelNode labelNode = new LabelNode();
            insnList.add(new JumpInsnNode(IFEQ,labelNode));
            insnList.add(new InsnNode(RETURN));
            insnList.add(labelNode);
            insnList.add(new FrameNode(F_SAME, 0, null, 0, null));
            methodNode.instructions.insert(insnList);

        }
    }

    public static boolean simpleNetworkWrapperHook(Object object, EnumMap enumMap){
        EventSendMessage eventSendMessage = new EventSendMessage(object,enumMap);
        EventManager.call(eventSendMessage);
        return eventSendMessage.isCancelled();
    }


    private byte[] transformMethods(byte[] bytes, BiConsumer<ClassNode, MethodNode> transformer) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        classNode.methods.forEach(m ->
                transformer.accept(classNode, m)
        );
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

}