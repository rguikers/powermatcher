package net.powermatcher.runtime;

import java.util.UUID;

import net.powermatcher.api.AgentEndpoint;
import net.powermatcher.api.MatcherEndpoint;
import net.powermatcher.api.Session;
import net.powermatcher.api.data.MarketBasis;
import net.powermatcher.api.messages.BidUpdate;
import net.powermatcher.api.messages.PriceUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionImpl
    implements Session {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionImpl.class);

    private final String sessionId;
    private final AgentEndpoint agentEndpoint;
    private final MatcherEndpoint matcherEndpoint;
    private final PotentialSession potentialSession;
    private final String agentId, matcherId, clusterId;
    private MarketBasis marketBasis;

    private volatile boolean connected;

    public SessionImpl(AgentEndpoint agentEndpoint, MatcherEndpoint matcherEndpoint, PotentialSession potentialSession) {
        sessionId = UUID.randomUUID().toString();
        this.agentEndpoint = agentEndpoint;
        this.matcherEndpoint = matcherEndpoint;
        this.potentialSession = potentialSession;

        agentId = agentEndpoint.getAgentId();
        matcherId = matcherEndpoint.getAgentId();
        clusterId = matcherEndpoint.getStatus().getClusterId();

        connected = false;
    }

    @Override
    public String getAgentId() {
        return agentId;
    }

    @Override
    public String getMatcherId() {
        return matcherId;
    }

    @Override
    public String getClusterId() {
        return clusterId;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public MarketBasis getMarketBasis() {
        return marketBasis;
    }

    @Override
    public void setMarketBasis(MarketBasis marketBasis) {
        if (this.marketBasis == null) {
            this.marketBasis = marketBasis;
        } else {
            throw new IllegalStateException("Received new MarketBasis for session; MarketBasis cannot be changed");
        }
    }

    void setConnected() {
        if (marketBasis == null) {
            throw new IllegalStateException("No MarketBasis has been set by the matcher [" + matcherId + "]");
        }
        connected = true;
    }

    @Override
    public synchronized void updatePrice(PriceUpdate priceUpdate) {
        if (connected) {
            agentEndpoint.handlePriceUpdate(priceUpdate);
        } else {
            LOGGER.debug("Sending a price update while not connected from agent [" + agentId + "]");
        }
    }

    @Override
    public synchronized void updateBid(BidUpdate bidUpdate) {
        if (connected) {
            matcherEndpoint.handleBidUpdate(this, bidUpdate);
        } else {
            LOGGER.debug("Sending a bid update while not connected from agent [" + agentId + "]");
        }
    }

    @Override
    public synchronized void disconnect() {
        connected = false;
        agentEndpoint.matcherEndpointDisconnected(this);
        matcherEndpoint.agentEndpointDisconnected(this);
        potentialSession.disconnected();
    }
}
