package tauri.dev.jsg.command.stargate;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.DimensionManager;
import tauri.dev.jsg.command.AbstractJSGCommand;
import tauri.dev.jsg.command.JSGCommand;
import tauri.dev.jsg.item.JSGItems;
import tauri.dev.jsg.item.notebook.PageNotebookItem;
import tauri.dev.jsg.stargate.network.StargateAddress;
import tauri.dev.jsg.stargate.network.StargateNetwork;
import tauri.dev.jsg.stargate.network.StargatePos;
import tauri.dev.jsg.stargate.network.SymbolTypeEnum;
import tauri.dev.jsg.tileentity.stargate.StargateAbstractBaseTile;
import tauri.dev.jsg.tileentity.stargate.StargateClassicBaseTile;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.command.CommandBase.parseCoordinate;

public class CommandStargateQuery extends AbstractJSGCommand {

    public CommandStargateQuery() {
        super(JSGCommand.JSG_BASE_COMMAND);
    }

    @Nonnull
    @Override
    public String getName() {
        return "sgquery";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Displays list of all gates";
    }

    @Nonnull
    @Override
    public String getGeneralUsage() {
        return "sgquery [x1 y1 z1 x2 y2 z2] [dim=id] [map=UNIVERSE|MILKYWAY|PEGASUS] [givepage=true|false]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
        AxisAlignedBB queryBox = null;
        SymbolTypeEnum symbolType = null;

        if (args.length >= 1 && args[0].equals("help")) {
            baseCommand.sendUsageMess(sender, this);
            return;
        }

        boolean checkDim = false;
        int dimId = 0;
        int idCheck = -1;
        boolean givePage = false;
        int givePageCount = 1;

        try {
            if (args.length >= 6) {
                BlockPos pos = sender.getPosition();
                int x1 = (int) parseCoordinate(pos.getX(), args[0], false).getResult();
                int y1 = (int) parseCoordinate(pos.getY(), args[1], 0, 255, false).getResult();
                int z1 = (int) parseCoordinate(pos.getZ(), args[2], false).getResult();
                int x2 = (int) parseCoordinate(pos.getX(), args[3], false).getResult();
                int y2 = (int) parseCoordinate(pos.getY(), args[4], 0, 255, false).getResult();
                int z2 = (int) parseCoordinate(pos.getZ(), args[5], false).getResult();

                BlockPos sPos = new BlockPos(x1, y1, z1);
                BlockPos tPos = new BlockPos(x2, y2, z2);

                queryBox = new AxisAlignedBB(sPos, tPos);

            }

            for (String arg : args) {
                if (arg.startsWith("dim=")) {
                    checkDim = true;
                    dimId = Integer.parseInt(arg.substring(4));
                } else if (arg.startsWith("map=")) {
                    symbolType = SymbolTypeEnum.valueOf(arg.substring(4).toUpperCase());
                } else if (arg.startsWith("id=")) {
                    idCheck = Integer.parseInt(arg.substring(3));
                } else if (arg.toLowerCase().startsWith("givepage=")) {
                    if (arg.length() > 8) {
                        // quantity given
                        try {
                            givePageCount = Integer.parseInt(arg.substring(9));
                        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                            //throw new WrongUsageException("commands.sgquery.wrong_quantity");
                            baseCommand.sendErrorMess(sender, "Wrong quantity!");
                            return;
                        }
                    }

                    givePage = true;
                }
            }

        } catch (NumberFormatException e) {
            baseCommand.sendErrorMess(sender, "Number expected!");
            return;
        } catch (IllegalArgumentException e) {
            baseCommand.sendErrorMess(sender, "No map!");
            return;
        }

        String infoString = "[dim=" + (checkDim ? dimId : "any") + ", ";
        infoString += "map=" + (symbolType != null ? symbolType.toString() : "no") + ", ";
        infoString += "id=" + (idCheck != -1 ? idCheck : "any") + ", ";
        infoString += "box=" + (queryBox != null ? queryBox.toString() : "any") + "]:";

        baseCommand.sendSuccessMess(sender, "Successfully executed!");
        baseCommand.sendRunningMess(sender, "Searching in: " + infoString);

        StargateNetwork network = StargateNetwork.get(sender.getEntityWorld());
        Map<StargateAddress, StargatePos> map = new HashMap<>(network.getMap().get(symbolType != null ? symbolType : SymbolTypeEnum.MILKYWAY));

        Map<StargatePos, Map<SymbolTypeEnum, StargateAddress>> map2 = network.getMapNotGenerated();

        for(StargatePos p : map2.keySet()){
            map.put(map2.get(p).get((symbolType != null ? symbolType : SymbolTypeEnum.MILKYWAY)), p);
        }

        int id = 1;
        StargateAddress selectedAddress = null;
        StargatePos selectedStargatePos = null;

        for (StargateAddress address : map.keySet()) {
            if (checkDim && map.get(address).dimensionID != dimId)
                continue;

            BlockPos pos = map.get(address).gatePos;

            if (queryBox != null && !queryBox.contains(new Vec3d(pos)))
                continue;

            if (idCheck == -1 || id == idCheck) {
                selectedStargatePos = map.get(address);
                selectedAddress = address;
                if(selectedStargatePos == null) continue;
                if(selectedAddress == null) continue;

                boolean isThere = network.isStargateInNetwork(address);

                StringBuilder gateString = new StringBuilder(" " + id + ". [");
                gateString.append("x=").append(isThere ? pos.getX() : "§k1").append(", ");
                gateString.append("y=").append(isThere ? pos.getY() : "§k1").append(", ");
                gateString.append("z=").append(isThere ? pos.getZ() : "§k1").append(", ");
                gateString.append("dim=").append(selectedStargatePos.dimensionID).append(" (").append(DimensionManager.getProviderType(selectedStargatePos.dimensionID).getName()).append(")").append(", ");
                gateString.append("isThere=").append(isThere ? "true" : "false").append("]");

                if (symbolType != null) {
                    gateString.append(": \n").append(TextFormatting.DARK_GRAY);

                    for (int i = 0; i < 8; i++) {
                        if (i >= 6)
                            gateString.append(TextFormatting.BLACK);

                        if (symbolType == SymbolTypeEnum.UNIVERSE)
                            gateString.append(address.get(i).toString());
                        else
                            gateString.append(address.get(i).localize());

                        if (i < 7)
                            gateString.append(", ");
                    }
                }

                baseCommand.sendInfoMess(sender, gateString.toString());

                if (symbolType != null)
                    baseCommand.sendInfoMess(sender, "");
            }

            id++;
        }

        if (givePage) {
            if (idCheck == -1 || selectedAddress == null || selectedStargatePos == null) {
                baseCommand.sendErrorMess(sender, "Wrong ID!");
                return;
            }

            if (symbolType == null) {
                baseCommand.sendErrorMess(sender, "Wrong map!");
                return;
            }

            if (!(sender instanceof EntityPlayer)) {
                baseCommand.sendErrorMess(sender, "Can not be executed through console!");
                return;
            }

            ItemStack stack = new ItemStack(JSGItems.PAGE_NOTEBOOK_ITEM, givePageCount, 1);
            int originId = -1;
            StargateAbstractBaseTile tile = selectedStargatePos.getTileEntity();
            if (tile instanceof StargateClassicBaseTile)
                originId = ((StargateClassicBaseTile) tile).getOriginId();
            stack.setTagCompound(PageNotebookItem.getCompoundFromAddress(selectedAddress, true, PageNotebookItem.getRegistryPathFromWorld(selectedStargatePos.getWorld(), selectedStargatePos.gatePos), originId));
            ((EntityPlayer) sender).addItemStackToInventory(stack);

            baseCommand.sendSuccessMess(sender, "Giving page...");
        }
    }

}
