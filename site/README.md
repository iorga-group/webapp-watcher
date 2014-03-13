# Installation

Install NPM :

```bash
sudo apt-get install npm
```

Install lesscss depdency :

```bash
npm install
```

Install ruby :

```bash
sudo apt-get install ruby1.9.3
```

Install rake and jekyll :

```bash
sudo gem install rake jekyll
```

And then you can build the site :

```bash
rake site:generate
```

And publish it :

```bash
rake site:publish
```

To test locally the website and devlop on it, run the following command :

```bash
jekyll serve -w --baseurl ''
```
