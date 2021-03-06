/**
 * ******************************************************************************************
 * Copyright (C) 2015 - Food and Agriculture Organization of the United Nations (FAO).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice,this list
 *       of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice,this list
 *       of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *    3. Neither the name of FAO nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,STRICT LIABILITY,OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *********************************************************************************************
 */
package org.sola.services.ejb.search.repository.entities;

import java.util.Date;
import javax.persistence.Column;
import org.sola.services.common.repository.CommonSqlProvider;
import org.sola.services.common.repository.entities.AbstractReadOnlyEntity;

public class ClaimSearchResult extends AbstractReadOnlyEntity {

    @Column
    private String id;
    @Column(name = "nr")
    private String nr;
    @Column(name = "lodgement_date")
    private Date lodgementDate;
    @Column(name = "challenge_expiry_date")
    private Date challengeExpiryDate;
    @Column(name = "decision_date")
    private Date decisionDate;
    @Column(name = "description")
    private String description;
    @Column(name = "challenged_claim_id")
    private String challengedClaimId;
    @Column(name = "claimant_id")
    private String claimantId;
    @Column(name = "claimant_name")
    private String claimantName;
    @Column(name = "status_code")
    private String statusCode;
    @Column(name = "status_name")
    private String statusName;

    public static final String PARAM_NAME = "claimantName";
    public static final String PARAM_DESCRIPTION = "claimDescription";
    public static final String PARAM_STATUS_CODE = "statusCode";
    public static final String PARAM_DATE_FROM = "dateFrom";
    public static final String PARAM_DATE_TO = "dateTo";
    public static final String PARAM_RECORDER = "recorderName";
    public static final String PARAM_POINT = "pointParam";
    private static final String SELECT_PART = 
            "select c.id, c.nr, c.lodgement_date, c.challenge_expiry_date, c.decision_date, c.description, \n"
            + "c.claimant_id, (cl.name || ' ' || coalesce(cl.last_name, '')) as claimant_name,\n"
            + "c.challenged_claim_id, c.status_code, get_translation(cs.display_value, #{" 
            + CommonSqlProvider.PARAM_LANGUAGE_CODE + "}) as status_name\n"
            + "\n"
            + "from (opentenure.claim c inner join opentenure.claim_status cs ON c.status_code = cs.code) \n"
            + "left join opentenure.claimant cl ON c.claimant_id = cl.id\n"
            + "\n";
    
    public static final String QUERY_SEARCH_BY_POINT = SELECT_PART
            + "WHERE c.challenged_claim_id is null and ST_Contains(c.mapped_geometry, ST_GeomFromText(#{" + PARAM_POINT + "}, St_SRID(c.mapped_geometry)));";
    
    public static final String QUERY_SEARCH = SELECT_PART
            + "where position(lower(#{" + PARAM_NAME + "}) in lower(cl.name || ' ' || COALESCE(cl.last_name, ''))) > 0 and\n"
            + "position(lower(#{" + PARAM_DESCRIPTION + "}) in lower(COALESCE(c.description, ''))) > 0 and \n"
            + "(c.status_code = #{" + PARAM_STATUS_CODE + "} or #{" + PARAM_STATUS_CODE + "} = '') and \n"
            + "c.lodgement_date between #{" + PARAM_DATE_FROM + "} and #{" + PARAM_DATE_TO + "} and "
            + "(c.recorder_name = #{" + PARAM_RECORDER + "} or #{" + PARAM_RECORDER + "} = '') "
            + "order by c.lodgement_date desc limit 100;";

    public ClaimSearchResult() {
        super();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNr() {
        return nr;
    }

    public void setNr(String nr) {
        this.nr = nr;
    }

    public Date getLodgementDate() {
        return lodgementDate;
    }

    public void setLodgementDate(Date lodgementDate) {
        this.lodgementDate = lodgementDate;
    }

    public Date getChallengeExpiryDate() {
        return challengeExpiryDate;
    }

    public void setChallengeExpiryDate(Date challengeExpiryDate) {
        this.challengeExpiryDate = challengeExpiryDate;
    }

    public Date getDecisionDate() {
        return decisionDate;
    }

    public void setDecisionDate(Date decisionDate) {
        this.decisionDate = decisionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChallengedClaimId() {
        return challengedClaimId;
    }

    public void setChallengedClaimId(String challengedClaimId) {
        this.challengedClaimId = challengedClaimId;
    }

    public String getClaimantId() {
        return claimantId;
    }

    public void setClaimantId(String claimantId) {
        this.claimantId = claimantId;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }
}
