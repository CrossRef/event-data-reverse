# Event Data Reverse

Service to transform URLs back into DOIs. This service is passive and almost-stateless.

## Usage

Provided as a Docker image with Docker Compose file for testing.

To run a demo:

    docker-compose -f docker-compose.yml run -w /usr/src/app -p "8004:8004" test lein run

To run tests

    docker-compose -f docker-compose.yml run -w /usr/src/app test lein test

## Config

| Environment variable | Description                         | Default | Required?                   |
|----------------------|-------------------------------------|---------|-----------------------------|
| `PORT`               | Port to listen on                   | 9990    | Yes                         |
| `STATUS_SERVICE`     | Public URL of the Status service    |         | No, ignored if not supplied |
| `JWT_SECRETS`        | Comma-separated list of JTW Secrets |         | Yes                         |


## Variously expressed DOIs

NB These URLs will not work until CED launched.

 - `10.5555/123456789` - a DOI expressed outside a URL. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=10.5555/123456789).
 - `doi: 10.5555/12345678` - a DOI expressed with a `doi:` prefix, flexibly. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=doi:%2010.5555/12345678).
 - `doi.org/10.5555/12345678` - a DOI expressed as a web address without full scheme. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=doi.org/10.5555/12345678).
 - `http://doi.org/10.5555/123456789` - a DOI expressed as a full URL. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/10.5555/123456789).

## URLs of landing pages where the DOI can be figured out from the URL
 
 - `http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0144297` - a landing page URL where the DOI is embedded in the URL as a GET query parameter. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0144297).
 - `http://www.jim.org.cn/EN/10.15541/jim20130556` - landing page where the DOI is included at the end of the URL. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://www.jim.org.cn/EN/10.15541/jim20130556)
 - `http://www.jstor.org/stable/10.13110/antipodes.27.2.0157#references_tab_contents` - landing page where the DOI is in the url but there's a hash fragment. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://www.jstor.org/stable/10.13110/antipodes.27.2.0157#references_tab_contents)
 - `http://www.nomos-elibrary.de/10.5235/219174411798862578/criminal-law-issues-in-the-case-law-of-the-european-court-of-justice-a-general-overview-jahrgang-1-2011-heft-2`  - URL with a DOI in it, but well buried. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://www.nomos-elibrary.de/10.5235/219174411798862578/criminal-law-issues-in-the-case-law-of-the-european-court-of-justice-a-general-overview-jahrgang-1-2011-heft-2)
 - `http://onlinelibrary.wiley.com/doi/10.1002/1521-3951(200009)221:1<453::AID-PSSB453>3.0.CO;2-Q/abstract;jsessionid=FAD5B5661A7D092460BEEDA0D55204DF.f02t01` - URL with a SICI in the DOI and a gnarly jsessionid too. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://onlinelibrary.wiley.com/doi/10.1002/1521-3951(200009)221:1<453::AID-PSSB453>3.0.CO;2-Q/abstract;jsessionid=FAD5B5661A7D092460BEEDA0D55204DF.f02t01)
 - `http://www.ijorcs.org/manuscript/id/12/doi:10.7815/ijorcs.21.2011.012/arul-anitha/network-security-using-linux-intrusion-detection-system` - URL with a DOI mixed with other text. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://www.ijorcs.org/manuscript/id/12/doi:10.7815/ijorcs.21.2011.012/arul-anitha/network-security-using-linux-intrusion-detection-system)
 - `http://link.springer.com/article/10.1007%2Fs10552-015-0707-0` - URL with embedded URL encoded DOI. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://link.springer.com/article/10.1007%2Fs10552-015-0707-0)
 - `http://doi.org/hvx` - ShortDOI as a URL [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/hvx)
 - `doi.org/hvx` - ShortDOI not as a URL [demo](https://reverse.eventdata.crossref.org/guess-doi?q=doi.org/hvx)
 - `10/hvx` - ShortDOI expressed as a Handle [demo](https://reverse.eventdata.crossref.org/guess-doi?q=10/hvx)

There will no doubt be a few other methods for figuring out the DOI from the URL in future.

## URLs of landing pages where the content of the page must be consulted

 - `http://www.hindawi.com/archive/2015/708915/` - a landing page where the DOI is mentioned in the page. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://www.hindawi.com/archive/2015/708915/).
 - `http://www.nature.com/nrendo/journal/v10/n9/full/nrendo.2014.114.html?WT.mc_id=TWT_NatureRevEndo` - landing page with a DOI, but there are extra bits on the end of the URL compared to the DOI link. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://www.nature.com/nrendo/journal/v10/n9/full/nrendo.2014.114.html?WT.mc_id=TWT_NatureRevEndo)

1. The textual and link content of the page are consulted and all DOIs are extracted.
2. The first DOI on the page is checked to see if it resolves back to the same URL.
3. After that, every other DOI is checked. If there are several matches (i.e. several DOIs resolve to the same URL) then the shortest DOI is chosen. This can happen with component DOIs, which in the case of e.g. PLOS, are extended versions of the base DOI.

Every DOI is checked against the API to ensure it exists.

## DOIs expressed outside URLs in plain text.

All of the above formats can be extracted from inside text.

 - `Look at this article lol 10.5555/12345678 what is a psycho ceramic neway?` - DOI mentioned in the text of a tweet. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=Look%20at%20this%20article%20lol%2010.5555/12345678%20what%20is%20a%20psycho%20ceramic%20neway?)
 - `Shrtr DOIs Mns Lgr Twts doi.org/hvx` - ShortDOI mentioned in the text of a tweet. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=Shrtr DOIs Mns Lgr Twts doi.org/hvx)
 - `Is This The Future of Concise Citation Identifiers? One Psychoceramicist Thinks So 10/hvx` - ShortDOI Handle mentioned in the text of a tweet. [demo](https://reverse.eventdata.crossref.org/guess-doi?q=Is%20This%20The%20Future%20of%20Concise%20Citation%20Identifiers?%20One%20Psychoceramicist%20Thinks%20So%2010/hvx)

## All the weird things mentioned in Geoff Bilder's *Appendix A: On the complexity of handling DOIs in the wild*

 - `10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23) - SICI with URLEncdoded trailing slash
 - `DOI%3A10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=DOI%3A10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://doi.org/10.1002/(SICI)1099-050X(199823/24)37:3/4<197::AID-HRM2>3.0.CO;2-#` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/10.1002/(SICI)1099-050X(199823/24)37:3/4<197::AID-HRM2>3.0.CO;2-#)
 - `http://doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://dx.doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://dx.doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://dx.doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://dx.doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://doi.org/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/bwhrfx)
 - `http://doi.org/doi:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/doi:bwhrfx)
 - `http://doi.org/urn:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/urn:bwhrfx)
 - `http://doi.org/info:doi/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/info:doi/bwhrfx)
 - `http://dx.doi.org/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/bwhrfx)
 - `http://dx.doi.org/doi:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/doi:bwhrfx)
 - `http://dx.doi.org/urn:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/urn:bwhrfx)
 - `http://dx.doi.org/info:doi/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/info:doi/bwhrfx)
 - `https://doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://dx.doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://dx.doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://dx.doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://dx.doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://doi.org/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/bwhrfx)
 - `https://doi.org/doi:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/doi:bwhrfx)
 - `https://doi.org/urn:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/urn:bwhrfx)
 - `https://doi.org/info:doi/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/info:doi/bwhrfx)
 - `https://dx.doi.org/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/bwhrfx)
 - `https://dx.doi.org/doi:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/doi:bwhrfx)
 - `https://dx.doi.org/urn:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/urn:bwhrfx)
 - `https://dx.doi.org/info:doi/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/info:doi/bwhrfx)
 - `doi:10.1002/(SICI)1099-050X(199823/24)37:3/4<197::AID-HRM2>3.0.CO;2-#` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=doi:10.1002/(SICI)1099-050X(199823/24)37:3/4<197::AID-HRM2>3.0.CO;2-#)
 - `DOI:10.1002/(SICI)1099-050X(199823/24)37:3/4<197::AID-HRM2>3.0.CO;2-#` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=DOI:10.1002/(SICI)1099-050X(199823/24)37:3/4<197::AID-HRM2>3.0.CO;2-#)
 - `http://doi.org/10.1002/(SICI)1099-050X(199823/24)37:3/4<197::AID-HRM2>3.0.CO;2-#` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/10.1002/(SICI)1099-050X(199823/24)37:3/4<197::AID-HRM2>3.0.CO;2-#)
 - `http://doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://dx.doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://dx.doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://dx.doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://dx.doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `http://doi.org/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/bwhrfx)
 - `http://doi.org/doi:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/doi:bwhrfx)
 - `http://doi.org/urn:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/urn:bwhrfx)
 - `http://doi.org/info:doi/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/info:doi/bwhrfx)
 - `http://dx.doi.org/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/bwhrfx)
 - `http://dx.doi.org/doi:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/doi:bwhrfx)
 - `http://dx.doi.org/urn:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/urn:bwhrfx)
 - `http://dx.doi.org/info:doi/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://dx.doi.org/info:doi/bwhrfx)
 - `https://doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://dx.doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://dx.doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://dx.doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/urn:doi:10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://dx.doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/info:doi/10.1002%2F(SICI)1099-050X(199823%2F24)37%3A3%2F4%3C197%3A%3AAID-HRM2%3E3.0.CO%3B2-%23)
 - `https://doi.org/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/bwhrfx)
 - `https://doi.org/doi:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/doi:bwhrfx)
 - `https://doi.org/urn:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/urn:bwhrfx)
 - `https://doi.org/info:doi/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/info:doi/bwhrfx)
 - `https://dx.doi.org/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/bwhrfx)
 - `https://dx.doi.org/doi:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/doi:bwhrfx)
 - `https://dx.doi.org/urn:bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/urn:bwhrfx)
 - `https://dx.doi.org/info:doi/bwhrfx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://dx.doi.org/info:doi/bwhrfx)
 - `10.1002/(sici)1099-050x(199823/24)37:3/4<197::aid-hrm2>3.0.co;2-#` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=10.1002/(sici)1099-050x(199823/24)37:3/4<197::aid-hrm2>3.0.co;2-#)
 - `10.1002/(SICI)1099-050x(199823/24)37:3/4<197::aid-hrm2>3.0.co;2-#` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=10.1002/(SICI)1099-050x(199823/24)37:3/4<197::aid-hrm2>3.0.co;2-#)
 - `10.1002/(sici)1099-050X(199823/24)37:3/4<197::AID-HRM2>3.0.CO;2-#` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=10.1002/(sici)1099-050X(199823/24)37:3/4<197::AID-HRM2>3.0.CO;2-#)
 - `http://doi.org/10.5555/12345678` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/10.5555/12345678)
 - `https://doi.org/10.5555/12345678` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/10.5555/12345678)
 - `http://doi.org/hvx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=http://doi.org/hvx)
 - `https://doi.org/hvx` [demo](https://reverse.eventdata.crossref.org/guess-doi?q=https://doi.org/hvx)

# Installation

- Create a mysql database with schema in `etc/schema.sql`.
- Create `config.edn` in `config/dev` or `config/prod` with `:database-name`, `:database-username`, `:database-host`.

# To run

Some tasks can be run on the command line but the main mode of operation is as a server.

    lein with-profile dev run server

To update the list of DOI and member IDs per publisher:

  lein run with-profile dev  update

To try and resolve the DOIs and extract domains. This will run until all DOIs are resolved (i.e. pretty much indefinitely).

  lein run with-profile dev resolve-all

## Ignored domains

Some domains are just too broad (like Google Pages). Maintain a list of these, in MySQL LIKE format in `resources/ignore.txt`. To update the database after a scan, or after updating the file:

    lein with-profile dev run mark-ignored


## TODO

### ACM

`http://dl.acm.org/citation.cfm?id=1852107` is quoted, DOI resolves to `http://dl.acm.org/citation.cfm?doid=1852102.1852107`.


## To install in production

1. Check out to `/home/deploy/doi-destinations`
2. `/home/deploy/doi-destinations` then `lein uberjar`
3. `sudo cp /home/deploy/doi-destinations/etc/doi-destinations.service /etc/systemd/system/doi-destinations.service`
4. `sudo systemctl enable doi-destinations.service`
5. `sudo systemctl start doi-destinations.service`

## License

Copyright © 2016 Crossref

Distributed under the MIT License.




## License

Copyright © Crossref

Distributed under the The MIT License (MIT).
