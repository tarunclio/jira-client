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

import org.json.JSONObject;

import java.util.Map;

/**
 * Represents a product version.
 */
public class Version extends Resource {

    private String name = null;
    private boolean archived = false;
    private boolean released = false;
    private String releaseDate;
    private String description = null;

    /**
     * Creates a version from a JSON payload.
     *
     * @param restclient REST client instance
     * @param json       JSON payload
     */
    protected Version(RestClient restclient, JSONObject json) {
        super(restclient);

        if (json != null && !json.isEmpty())
            deserialise(json);
    }
    
    /**
     * Merges the given version with current version
     * 
     * @param version
     *            The version to merge
     */
    public void mergeWith(Version version) {
    
        JSONObject req = new JSONObject();
        req.put("description", version.getDescription());
        req.put("name", version.getName());
        req.put("archived", version.isArchived());
        req.put("released", version.isReleased());
        req.put("releaseDate", version.getReleaseDate());

        try {
            restclient.put(Resource.getBaseUri() + "version/" + id, req);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to merge", ex);
        }
    }

    /**
    * Copies the version to the given project
    * 
    * @param project
    *            The project the version will be copied to
    */
    public void copyTo(Project project) {
    
        JSONObject req = new JSONObject();
        req.put("description", getDescription());
        req.put("name", getName());
        req.put("archived", isArchived());
        req.put("released", isReleased());
        req.put("releaseDate", getReleaseDate());
        req.put("project", project.getKey());
        req.put("projectId", project.getId());
      
        try {
            restclient.post(Resource.getBaseUri() + "version/", req);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to copy to project '" + project.getKey() + "'", ex);
        }
    }

    /**
     * Retrieves the given version record.
     *
     * @param restclient REST client instance
     * @param id         Internal JIRA ID of the version
     * @return a version instance
     * @throws JiraException when the retrieval fails
     */
    public static Version get(RestClient restclient, String id)
            throws JiraException {

        JSONObject result = null;

        try {
            result = restclient.get(getBaseUri() + "version/" + id);
        } catch (Exception ex) {
            throw new JiraException("Failed to retrieve version " + id, ex);
        }

        if (!(result instanceof JSONObject))
            throw new JiraException("JSON payload is malformed");

        return new Version(restclient, (JSONObject) result);
    }

    private void deserialise(JSONObject json) {
        Map map = json.toMap();

        self = json.optString("self");
        id = json.optString("id");
        name = json.optString("name");
        archived = json.optBoolean("archived");
        released = json.optBoolean("released");
        releaseDate = json.optString("releaseDate");
        description = json.optString("description");
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public boolean isArchived() {
        return archived;
    }

    public boolean isReleased() {
        return released;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getDescription() {
        return description;

    }
}

