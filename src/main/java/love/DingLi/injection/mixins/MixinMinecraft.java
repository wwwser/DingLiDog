package love.DingLi.injection.mixins;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.BlockInteractEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.event.events.WorldEvent;
import baritone.api.event.events.type.EventState;
import com.darkmagician6.eventapi.EventManager;

import love.DingLi.DingLiDog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import love.DingLi.events.*;
import java.util.function.BiFunction;

@Mixin(Minecraft.class)
@SideOnly(Side.CLIENT)
public abstract class MixinMinecraft {
    @Shadow
    public GuiScreen currentScreen;
    @Shadow
    public boolean skipRenderWorld;
    @Shadow
    public RayTraceResult objectMouseOver;
    @Shadow
    public WorldClient world;
    @Shadow
    public EntityPlayerSP player;
    @Shadow
    public ParticleManager effectRenderer;
    @Shadow
    public PlayerControllerMP playerController;

    @Unique
    private BiFunction<EventState, TickEvent.Type, TickEvent> tickProvider;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V", ordinal = 2, shift = At.Shift.AFTER))
    private void startGame(CallbackInfo callbackInfo) {
        new DingLiDog().startClient();
    }
    @Inject(method = "runTick", at = @At("HEAD"))
    private void onTick(final CallbackInfo callbackInfo) {
        EventManager.call(new love.DingLi.events.TickEvent());
    }
    @Inject(method = "runTickKeyboard", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V", shift = At.Shift.AFTER))
    private void onKey(CallbackInfo callbackInfo) {
        if (Keyboard.getEventKeyState() && currentScreen == null)
            EventManager.call(new KeyEvent(Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey()));
    }
    @Inject(method = "displayGuiScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;", shift = At.Shift.AFTER))
    private void displayGuiScreen(CallbackInfo callbackInfo) {
        if(currentScreen instanceof GuiMainMenu || (currentScreen != null && currentScreen.getClass().getName().startsWith("net.labymod") && currentScreen.getClass().getSimpleName().equals("ModGuiMainMenu"))) {
            currentScreen = new GuiMainMenu();

            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            currentScreen.setWorldAndResolution(Minecraft.getMinecraft(), scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
            skipRenderWorld = false;
        }

        EventManager.call(new ScreenEvent(currentScreen));
    }

    @Inject(
            method = "init",
            at = @At("RETURN")
    )
    private void postInit(CallbackInfo ci) {
        BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    @Inject(
            method = "runTick",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "net/minecraft/client/Minecraft.currentScreen:Lnet/minecraft/client/gui/GuiScreen;",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            slice = @Slice(
                    from = @At(
                            value = "FIELD",
                            opcode = Opcodes.PUTFIELD,
                            target = "net/minecraft/client/Minecraft.leftClickCounter:I"
                    )
            )
    )
    private void runTick(CallbackInfo ci) {
        this.tickProvider = TickEvent.createNextProvider();

        for (IBaritone baritone : BaritoneAPI.getProvider().getAllBaritones()) {
            TickEvent.Type type = baritone.getPlayerContext().player() != null && baritone.getPlayerContext().world() != null
                    ? TickEvent.Type.IN
                    : TickEvent.Type.OUT;
            baritone.getGameEventHandler().onTick(this.tickProvider.apply(EventState.PRE, type));
        }
    }

    @Inject(
            method = "runTick",
            at = @At("RETURN")
    )
    private void postRunTick(CallbackInfo ci) {
        if (this.tickProvider == null) {
            return;
        }

        for (IBaritone baritone : BaritoneAPI.getProvider().getAllBaritones()) {
            TickEvent.Type type = baritone.getPlayerContext().player() != null && baritone.getPlayerContext().world() != null
                    ? TickEvent.Type.IN
                    : TickEvent.Type.OUT;
            baritone.getGameEventHandler().onPostTick(this.tickProvider.apply(EventState.POST, type));
        }

        this.tickProvider = null;
    }

    @Inject(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/multiplayer/WorldClient.updateEntities()V",
                    shift = At.Shift.AFTER
            )
    )
    private void postUpdateEntities(CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(this.player);
        if (baritone != null) {
            // Intentionally call this after all entities have been updated. That way, any modification to rotations
            // can be recognized by other entity code. (Fireworks and Pigs, for example)
            baritone.getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.POST));
        }
    }

    @Inject(
            method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
            at = @At("HEAD")
    )
    private void preLoadWorld(WorldClient world, String loadingMessage, CallbackInfo ci) {
        // If we're unloading the world but one doesn't exist, ignore it
        if (this.world == null && world == null) {
            return;
        }

        // mc.world changing is only the primary baritone

        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onWorldEvent(
                new WorldEvent(
                        world,
                        EventState.PRE
                )
        );
    }

    @Inject(
            method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
            at = @At("RETURN")
    )
    private void postLoadWorld(WorldClient world, String loadingMessage, CallbackInfo ci) {
        // still fire event for both null, as that means we've just finished exiting a world

        // mc.world changing is only the primary baritone
        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onWorldEvent(
                new WorldEvent(
                        world,
                        EventState.POST
                )
        );
    }

    @Redirect(
            method = "runTick",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "net/minecraft/client/gui/GuiScreen.allowUserInput:Z"
            )
    )
    private boolean isAllowUserInput(GuiScreen screen) {
        // allow user input is only the primary baritone
        return (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing() && player != null) || screen.allowUserInput;
    }

    @Inject(
            method = "clickMouse",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/multiplayer/PlayerControllerMP.clickBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onBlockBreak(CallbackInfo ci, BlockPos pos) {
        // clickMouse is only for the main player
        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onBlockInteract(new BlockInteractEvent(pos, BlockInteractEvent.Type.START_BREAK));
    }

    @Inject(
            method = "rightClickMouse",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/entity/EntityPlayerSP.swingArm(Lnet/minecraft/util/EnumHand;)V"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onBlockUse(CallbackInfo ci, EnumHand var1[], int var2, int var3, EnumHand enumhand, ItemStack itemstack, BlockPos blockpos, int i, EnumActionResult enumactionresult) {
        // rightClickMouse is only for the main player
        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onBlockInteract(new BlockInteractEvent(blockpos, BlockInteractEvent.Type.USE));
    }

}
