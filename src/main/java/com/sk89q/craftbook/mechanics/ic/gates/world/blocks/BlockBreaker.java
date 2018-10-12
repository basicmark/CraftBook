package com.sk89q.craftbook.mechanics.ic.gates.world.blocks;

import com.google.common.collect.Lists;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.bukkit.util.CraftBookBukkitUtil;
import com.sk89q.craftbook.mechanics.ic.AbstractICFactory;
import com.sk89q.craftbook.mechanics.ic.AbstractSelfTriggeredIC;
import com.sk89q.craftbook.mechanics.ic.ChipState;
import com.sk89q.craftbook.mechanics.ic.ConfigurableIC;
import com.sk89q.craftbook.mechanics.ic.IC;
import com.sk89q.craftbook.mechanics.ic.ICFactory;
import com.sk89q.craftbook.mechanics.ic.ICVerificationException;
import com.sk89q.craftbook.util.BlockSyntax;
import com.sk89q.craftbook.util.BlockUtil;
import com.sk89q.craftbook.util.ICUtil;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.Blocks;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BlockBreaker extends AbstractSelfTriggeredIC {

    public BlockBreaker(Server server, ChangedSign block, ICFactory factory) {

        super(server, block, factory);
    }

    @Override
    public String getTitle() {

        return "Block Breaker";
    }

    @Override
    public String getSignTitle() {

        return "BLOCK BREAK";
    }

    @Override
    public void trigger(ChipState chip) {

        if (chip.getInput(0)) {
            chip.setOutput(0, breakBlock());
        }
    }

    @Override
    public void think(ChipState state) {

        state.setOutput(0, breakBlock());
    }

    private Block broken, chest;
    private BlockStateHolder item;

    @Override
    public void load() {
        item = BlockSyntax.getBlock(getLine(2), true);
    }

    public boolean breakBlock() {

        if (broken == null) {

            Block bl = getBackBlock();

            if (((Factory)getFactory()).above) {
                broken = bl.getRelative(0, -1, 0);
            } else {
                broken = bl.getRelative(0, 1, 0);
            }
        }

        if (broken == null || broken.getType() == Material.AIR || broken.getType() == Material.MOVING_PISTON || Blocks
                .containsFuzzy(((Factory) getFactory()).blockBlacklist, BukkitAdapter.adapt(broken.getBlockData())))
            return false;

        if (!item.equalsFuzzy(BukkitAdapter.adapt(broken.getBlockData())))

        ICUtil.collectItem(this, new Vector(0, 1, 0), BlockUtil.getBlockDrops(broken, null));
        broken.setType(Material.AIR);

        return true;
    }

    public void dropItem(ItemStack item) {

        CraftBookBukkitUtil.toSign(getSign()).getWorld().dropItem(BlockUtil.getBlockCentre(CraftBookBukkitUtil.toSign(getSign()).getBlock()), item);
    }

    public static class Factory extends AbstractICFactory implements ConfigurableIC {

        boolean above;

        List<BlockStateHolder> blockBlacklist;

        public Factory(Server server, boolean above) {

            super(server);
            this.above = above;
        }

        @Override
        public IC create(ChangedSign sign) {

            return new BlockBreaker(getServer(), sign, this);
        }

        @Override
        public void verify(ChangedSign sign) throws ICVerificationException {

            if(!sign.getLine(2).trim().isEmpty()) {
                BlockStateHolder item = BlockSyntax.getBlock(sign.getLine(2), true);
                if(item == null)
                    throw new ICVerificationException("An invalid block was provided on line 2!");
                if(blockBlacklist.contains(item))
                    throw new ICVerificationException("A blacklisted block was provided on line 2!");
            }
        }

        @Override
        public String getShortDescription() {

            return "Breaks blocks " + (above ? "above" : "below") + " block sign is on.";
        }

        @Override
        public String[] getLineHelp() {

            return new String[] {"+oBlock ID:Data", null};
        }

        @Override
        public void addConfiguration (YAMLProcessor config, String path) {

            config.setComment(path + "blacklist", "Stops the IC from breaking the listed blocks.");
            blockBlacklist = BlockSyntax.getBlocks(config.getStringList(path + "blacklist", Lists.newArrayList(BlockTypes.BEDROCK.getId())), true);
        }
    }
}