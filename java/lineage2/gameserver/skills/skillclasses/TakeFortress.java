/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package lineage2.gameserver.skills.skillclasses;

import java.util.List;
import lineage2.gameserver.model.Creature;
import lineage2.gameserver.model.GameObject;
import lineage2.gameserver.model.Player;
import lineage2.gameserver.model.Skill;
import lineage2.gameserver.model.World;
import lineage2.gameserver.model.entity.events.impl.FortressSiegeEvent;
import lineage2.gameserver.model.entity.events.impl.SiegeEvent;
import lineage2.gameserver.model.entity.events.objects.FortressCombatFlagObject;
import lineage2.gameserver.model.entity.events.objects.StaticObjectObject;
import lineage2.gameserver.model.instances.StaticObjectInstance;
import lineage2.gameserver.model.items.attachment.ItemAttachment;
import lineage2.gameserver.network.serverpackets.SystemMessage2;
import lineage2.gameserver.network.serverpackets.components.SystemMsg;
import lineage2.gameserver.templates.StatsSet;

/**
 * @author Mobius
 * @version $Revision: 1.0 $
 */
public class TakeFortress extends Skill
{
	/**
	 * Constructor for TakeFortress.
	 * @param set StatsSet
	 */
	public TakeFortress(StatsSet set)
	{
		super(set);
	}
	
	/**
	 * Method checkCondition.
	 * @param activeChar Creature
	 * @param target Creature
	 * @param forceUse boolean
	 * @param dontMove boolean
	 * @param first boolean
	 * @return boolean
	 */
	@Override
	public boolean checkCondition(Creature activeChar, Creature target, boolean forceUse, boolean dontMove, boolean first)
	{
		if (!super.checkCondition(activeChar, target, forceUse, dontMove, first))
		{
			return false;
		}
		
		if ((activeChar == null) || !activeChar.isPlayer())
		{
			return false;
		}
		
		GameObject flagPole = activeChar.getTarget();
		
		if (!(flagPole instanceof StaticObjectInstance) || (((StaticObjectInstance) flagPole).getType() != 3))
		{
			activeChar.sendPacket(SystemMsg.THE_TARGET_IS_NOT_A_FLAGPOLE_SO_A_FLAG_CANNOT_BE_DISPLAYED);
			return false;
		}
		
		if (first)
		{
			List<Creature> around = World.getAroundCharacters(flagPole, getSkillRadius() * 2, 100);
			
			for (Creature ch : around)
			{
				if (ch.isCastingNow() && (ch.getCastingSkill() == this))
				{
					activeChar.sendPacket(SystemMsg.A_FLAG_IS_ALREADY_BEING_DISPLAYED_ANOTHER_FLAG_CANNOT_BE_DISPLAYED);
					return false;
				}
			}
		}
		
		Player player = (Player) activeChar;
		
		if (player.getClan() == null)
		{
			activeChar.sendPacket(new SystemMessage2(SystemMsg.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(this));
			return false;
		}
		
		FortressSiegeEvent siegeEvent = player.getEvent(FortressSiegeEvent.class);
		
		if (siegeEvent == null)
		{
			activeChar.sendPacket(new SystemMessage2(SystemMsg.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(this));
			return false;
		}
		
		if (player.isMounted())
		{
			activeChar.sendPacket(new SystemMessage2(SystemMsg.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(this));
			return false;
		}
		
		ItemAttachment attach = player.getActiveWeaponFlagAttachment();
		
		if (!(attach instanceof FortressCombatFlagObject) || (((FortressCombatFlagObject) attach).getEvent() != siegeEvent))
		{
			activeChar.sendPacket(new SystemMessage2(SystemMsg.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(this));
			return false;
		}
		
		if (!player.isInRangeZ(target, getCastRange()))
		{
			player.sendPacket(SystemMsg.YOUR_TARGET_IS_OUT_OF_RANGE);
			return false;
		}
		
		if (first)
		{
			siegeEvent.broadcastTo(new SystemMessage2(SystemMsg.S1_CLAN_IS_TRYING_TO_DISPLAY_A_FLAG).addString(player.getClan().getName()), SiegeEvent.DEFENDERS);
		}
		
		return true;
	}
	
	/**
	 * Method useSkill.
	 * @param activeChar Creature
	 * @param targets List<Creature>
	 */
	@Override
	public void useSkill(Creature activeChar, List<Creature> targets)
	{
		GameObject flagPole = activeChar.getTarget();
		
		if (!(flagPole instanceof StaticObjectInstance) || (((StaticObjectInstance) flagPole).getType() != 3))
		{
			return;
		}
		
		Player player = (Player) activeChar;
		FortressSiegeEvent siegeEvent = player.getEvent(FortressSiegeEvent.class);
		
		if (siegeEvent == null)
		{
			return;
		}
		
		StaticObjectObject object = siegeEvent.getFirstObject(FortressSiegeEvent.FLAG_POLE);
		
		if (((StaticObjectInstance) flagPole).getUId() != object.getUId())
		{
			return;
		}
		
		siegeEvent.processStep(player.getClan());
	}
}
