/*
 * Copyright 2016 RedRoma, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.redroma.yelp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.redroma.yelp.exceptions.YelpBadArgumentException;
import tech.redroma.yelp.exceptions.YelpOperationFailedException;
import tech.sirwellington.alchemy.annotations.access.Internal;
import tech.sirwellington.alchemy.http.AlchemyHttp;
import tech.sirwellington.alchemy.http.HttpResponse;
import tech.sirwellington.alchemy.http.exceptions.AlchemyHttpException;

import static tech.sirwellington.alchemy.arguments.Arguments.checkThat;
import static tech.sirwellington.alchemy.arguments.assertions.Assertions.notNull;
import static tech.sirwellington.alchemy.arguments.assertions.BooleanAssertions.trueStatement;
import static tech.sirwellington.alchemy.arguments.assertions.StringAssertions.nonEmptyString;
import static tech.sirwellington.alchemy.arguments.assertions.StringAssertions.stringWithLengthGreaterThan;

/**
 *
 * @author SirWellington
 */
@Internal
final class OAuthTokenProviderRenewing implements OAuthTokenProvider
{

    private static final Logger LOG = LoggerFactory.getLogger(OAuthTokenProviderRenewing.class);

    private final AlchemyHttp http;
    private final URL authenticationURL;
    private final String clientId;
    private final String clientSecret;
    private static final int BAD_REQUEST = 400;

    OAuthTokenProviderRenewing(AlchemyHttp http, URL authenticationURL, String clientId, String clientSecret)
    {
        checkThat(http, authenticationURL)
            .are(notNull());
        
        checkThat(clientId, clientSecret)
            .are(nonEmptyString())
            .are(stringWithLengthGreaterThan(2));

        this.http = http;
        this.authenticationURL = authenticationURL;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String getToken()
    {
        JsonObject response = null;
        try
        {
            response = http
                .go()
                .post()
                .nothing()
                .usingQueryParam(Keys.GRANT_TYPE, "client_credentials")
                .usingQueryParam(Keys.CLIENT_ID, clientId)
                .usingQueryParam(Keys.CLIENT_SECRET, clientSecret)
                .usingHeader(Keys.CONTENT_TYPE, Keys.URL_ENCODED)
                .expecting(JsonObject.class)
                .at(authenticationURL);
        }
        catch (AlchemyHttpException ex)
        {
            HttpResponse yelpResponse = ex.getResponse();
            int status = yelpResponse.statusCode();
            
            if (status == BAD_REQUEST)
            {
                throw new YelpBadArgumentException("Client ID or Secret are incorrect: " + yelpResponse.body());
            }
            
            throw new YelpOperationFailedException("Failed to get access token.", ex);
        }
        
        checkResponse(response);
        printExpiration(response);
        
        return tryToGetTokenFrom(response);
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.http);
        hash = 47 * hash + Objects.hashCode(this.authenticationURL);
        hash = 47 * hash + Objects.hashCode(this.clientId);
        hash = 47 * hash + Objects.hashCode(this.clientSecret);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final OAuthTokenProviderRenewing other = (OAuthTokenProviderRenewing) obj;
        if (!Objects.equals(this.clientId, other.clientId))
        {
            return false;
        }
        if (!Objects.equals(this.clientSecret, other.clientSecret))
        {
            return false;
        }
        if (!Objects.equals(this.http, other.http))
        {
            return false;
        }
        if (!Objects.equals(this.authenticationURL, other.authenticationURL))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "OAuthTokenProviderRenewing{" + "http=" + http + ", authenticationURL=" + authenticationURL + ", clientId=" + clientId + ", clientSecret=" + "<redacted>" + '}';
    }

    private void printExpiration(JsonObject response)
    {
        JsonElement expiration = response.get(Keys.EXPIRATION);
        
        if (expiration.isJsonPrimitive())
        {
            JsonPrimitive expirationValue = expiration.getAsJsonPrimitive();
            
            if (expirationValue.isNumber())
            {
                int expirationSeconds = expirationValue.getAsInt();
                long expirationDays = TimeUnit.SECONDS.toDays(expirationSeconds);
                long expirationMinutes = TimeUnit.SECONDS.toMinutes(expirationSeconds);
                
                LOG.debug("Yelp Token expires in {} days or {} minutes", expirationDays, expirationMinutes);
            }
            else
            {
                LOG.warn("Received unexpected token expiration: " + expirationValue);
            }
        }
    }

    private void checkResponse(JsonObject response)
    {
        checkThat(response)
            .throwing(YelpOperationFailedException.class)
            .usingMessage("Received unexpected null response")
            .is(notNull());
        
        checkThat(response.has(Keys.EXPIRATION))
            .usingMessage("OAuth response is missing expiration information: " + response)
            .is(trueStatement());
        
        checkThat(response.has(Keys.TOKEN)).usingMessage("OAUth response is missing access token: " + response)
            .throwing(YelpOperationFailedException.class)
            .is(trueStatement());
    }

    private String tryToGetTokenFrom(JsonObject response)
    {
        checkThat(response.has(Keys.TOKEN))
            .usingMessage("Yelp AUTH response is missing the token")
            .throwing(YelpBadArgumentException.class)
            .is(trueStatement());
        
        return response.get(Keys.TOKEN).getAsString();
    }

    private static class Keys
    {
        static final String GRANT_TYPE = "grant_type";
        static final String CLIENT_ID = "client_id";
        static final String CLIENT_SECRET = "client_secret";
        static final String CONTENT_TYPE = "Content-Type";
        static final String URL_ENCODED = "application/x-www-form-urlencoded";
        
        //Response Keys
        static final String EXPIRATION = "expires_in";
        static final String TOKEN = "access_token";
    }

}
