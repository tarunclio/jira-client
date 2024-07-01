/**
 * jira-client - a simple JIRA REST client
 * Copyright (c) 2013 Bob Carroll (bob.carroll@alum.rit.edu)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.rcarz.jiraclient;

import java.util.Map;

import org.json.JSONObject;

/**
 * Represents an issue link type.
 */
public class LinkType extends Resource {

    private String name = null;
    private String inward = null;
    private String outward = null;

    /**
     * Creates a issue link type from a JSON payload.
     *
     * @param restclient REST client instance
     * @param json JSON payload
     */
    protected LinkType(RestClient restclient, JSONObject json) {
        super(restclient);

        if (json != null && ! json.isEmpty())
            deserialise(json);
    }

    private void deserialise(JSONObject json) {
        Map map = json.toMap();

        self = json.optString(("self"));
        id = json.optString(("id"));
        name = json.optString(("name"));
        inward = json.optString(("inward"));
        outward = json.optString(("outward"));
    }

    /**
     * Retrieves the given issue link type record.
     *
     * @param restclient REST client instance
     * @param id Internal JIRA ID of the issue link type
     *
     * @return a issue link type instance
     *
     * @throws JiraException when the retrieval fails
     */
    public static LinkType get(RestClient restclient, String id)
        throws JiraException {

        JSONObject result = null;

        try {
            result = restclient.get(getBaseUri() + "issueLinkType/" + id);
        } catch (Exception ex) {
            throw new JiraException("Failed to retrieve issue link type " + id, ex);
        }

        if (!(result instanceof JSONObject))
            throw new JiraException("JSON payload is malformed");

        return new LinkType(restclient, (JSONObject)result);
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public String getInward() {
        return inward;
    }

    public String getOutward() {
        return outward;
    }
}

