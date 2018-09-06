/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myriad.scheduler.fgs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.myriad.driver.model.MesosV1;

/**
 * Represents offers from a slave that have been consumed by Myriad.
 */
public class ConsumedOffer {
  private List<MesosV1.Offer> offers;

  public ConsumedOffer() {
    this.offers = new LinkedList<>();
  }

  public void add(MesosV1.Offer offer) {
    offers.add(offer);
  }

  public List<MesosV1.Offer> getOffers() {
    return offers;
  }

  public Collection<MesosV1.OfferID> getOfferIds() {
    Collection<MesosV1.OfferID> ids = new ArrayList<>(offers.size());

    for (MesosV1.Offer offer : offers) {
      ids.add(offer.getId());
    }

    return ids;
  }
}
