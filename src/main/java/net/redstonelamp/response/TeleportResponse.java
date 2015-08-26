/**
 * This file is part of RedstoneLamp.
 * <p>
 * RedstoneLamp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * RedstoneLamp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with RedstoneLamp.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.redstonelamp.response;

import net.redstonelamp.level.position.Position;

/**
 * A Response that teleports the player to the given position.
 *
 * @author RedstoneLamp Team
 */
public class TeleportResponse extends Response{
    public Position pos;
    public float bodyYaw;
    public boolean onGround;

    public TeleportResponse(Position pos, boolean onGround){
        this.pos = pos;
        this.onGround = onGround;
        bodyYaw = pos.getYaw();
    }
}
