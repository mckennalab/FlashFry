/*
 *
 *     Copyright (C) 2017  Aaron McKenna
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package crispr

import org.scalatest.{FlatSpec, Matchers}

class GuideMemoryStorageTest extends FlatSpec with Matchers {

  "GuideMemoryStorageTest" should "filter by gc" in {
    val gm = new GuideMemoryStorage()

    val chit = new CRISPRSite("test", "AAAAA", true, 1, None)
    gm.addHit(chit)
    assert(gm.guideHits.size == 1)

    val filtered = GuideMemoryStorage.filter_by_GC(gm,0.1,1.0)
    assert(filtered.guideHits.size == 0)
  }

  "GuideMemoryStorageTest" should "not filter when it doesn't need to" in {
    val gm = new GuideMemoryStorage()

    val chit = new CRISPRSite("test", "AAAAA", true, 1, None)
    gm.addHit(chit)
    val chit2 = new CRISPRSite("test", "GGGGG", true, 1, None)
    gm.addHit(chit2)
    assert(gm.guideHits.size == 2)

    val filtered = GuideMemoryStorage.filter_by_GC(gm,0.0,1.0)
    assert(filtered.guideHits.size == 2)
  }
}