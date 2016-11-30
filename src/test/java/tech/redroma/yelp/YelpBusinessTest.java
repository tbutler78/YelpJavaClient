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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner;
import tech.sirwellington.alchemy.test.junit.runners.DontRepeat;
import tech.sirwellington.alchemy.test.junit.runners.GeneratePojo;
import tech.sirwellington.alchemy.test.junit.runners.Repeat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static tech.sirwellington.alchemy.arguments.Arguments.checkThat;
import static tech.sirwellington.alchemy.arguments.assertions.GeolocationAssertions.validLatitude;
import static tech.sirwellington.alchemy.arguments.assertions.GeolocationAssertions.validLongitude;
import static tech.sirwellington.alchemy.generator.AlchemyGenerator.one;
import static tech.sirwellington.alchemy.generator.NumberGenerators.doubles;

/**
 *
 * @author SirWellington
 */
@Repeat(10)
@RunWith(AlchemyTestRunner.class)
public class YelpBusinessTest
{

    @GeneratePojo
    private YelpBusiness instance;

    @GeneratePojo
    private YelpBusiness first;

    @GeneratePojo
    private YelpBusiness second;

    @Before
    public void setUp()
    {
        setupData();
    }

    private void setupData()
    {
        double latitude = one(doubles(-90, 90));
        double longitude = one(doubles(-180, 180));
        instance.coordinates.latitude = latitude;
        instance.coordinates.longitude = longitude;
    }

    @DontRepeat
    @Test
    public void testInstance()
    {
        assertThat(instance, notNullValue());
    }

    @Test
    public void testInstanceHasAllData()
    {
        checkBusiness(instance);
        checkBusiness(first);
        checkBusiness(second);
    }

    private void checkBusiness(YelpBusiness business)
    {
        assertThat(business.id, not(isEmptyOrNullString()));
        assertThat(business.imageURL, not(isEmptyOrNullString()));
        assertThat(business.name, not(isEmptyOrNullString()));
        assertThat(business.phone, not(isEmptyOrNullString()));
        assertThat(business.url, not(isEmptyOrNullString()));
        assertThat(business.isClosed, notNullValue());
        assertThat(business.distance, notNullValue());
        assertThat(business.categories, notNullValue());

        business.categories.forEach(this::checkCategory);
        checkCoordinate(business.coordinates);
        checkAddress(business.location);
    }

    private void checkCategory(Category category)
    {
        assertThat(category, notNullValue());
        assertThat(category.alias, not(isEmptyOrNullString()));
        assertThat(category.title, not(isEmptyOrNullString()));
    }

    private void checkCoordinate(Coordinate coordinate)
    {
        assertThat(coordinate, notNullValue());
        checkThat(coordinate.latitude)
            .is(validLatitude());
        checkThat(coordinate.longitude)
            .is(validLongitude());
    }

    private void checkAddress(Address address)
    {
        assertThat(address, notNullValue());
        assertThat(address.address1, not(isEmptyOrNullString()));
        assertThat(address.city, not(isEmptyOrNullString()));
        assertThat(address.country, not(isEmptyOrNullString()));
        assertThat(address.state, not(isEmptyOrNullString()));
        assertThat(address.zipCode, not(isEmptyOrNullString()));
    }

    @Test
    public void testEqualsWhenNotEqual()
    {
        assertThat(first, not(second));
    }

    @Test
    public void testEqualsWhenEquals()
    {
        assertThat(first, is(first));
    }

    @Test
    public void testHashCode()
    {
        assertThat(first.hashCode(), is(first.hashCode()));
        assertThat(first.hashCode(), not(second.hashCode()));
    }

}
